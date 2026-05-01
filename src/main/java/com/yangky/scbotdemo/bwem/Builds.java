package com.yangky.scbotdemo.bwem;

import bwapi.Race;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import com.yangky.scbotdemo.util.Positions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Builds {
    private static final List<BuildTask> taskList = new LinkedList<>();
    private static final Object lock = new Object();

    public static boolean idempotent(String idempotentNo) {
        return taskList.stream().anyMatch(e -> StringUtils.equals(e.getIdempotentNo(), idempotentNo));
    }

    public static void add(BuildTask task) {
        System.out.println("[DEBUG] 尝试添加任务: " + task.getIdempotentNo() + ", 当前任务数: " + taskList.size());
        // ✅ 先检查该位置是否已有建造任务
        boolean hasTaskAtPosition = taskList.stream()
                .anyMatch(t -> t.buildPosition.equals(task.buildPosition) && !t.submitted);
        if (hasTaskAtPosition) {
            System.out.println("该位置已有建造任务，跳过: " + task.buildPosition);
            return;
        }
        // 先检查幂等，避免重复添加
        if (idempotent(task.getIdempotentNo())) {
            System.out.println("任务已存在，跳过: " + task.getIdempotentNo());
            return;
        }
        System.out.println("[DEBUG] 幂等检查通过，准备分配工人");
        // 获取一个可用的工人
        if (task.worker == null) {
            task.worker = Workers.getAWorker(Games.game.self(), task.buildPosition.toPosition());
            if (task.worker == null) {
                System.out.println("没有可用工人");
                return;
            }
            System.out.println("[DEBUG] 分配工人: " + task.worker.getID());
        }
        // 再次检查幂等（防止在分配工人期间被其他线程添加）
        if (idempotent(task.getIdempotentNo())) {
            System.out.println("任务已存在，跳过: " + task.getIdempotentNo());
            return;
        }
        // ✅ 再次检查该位置是否已有建造任务（双重检查）
        hasTaskAtPosition = taskList.stream()
                .anyMatch(t -> t.buildPosition.equals(task.buildPosition) && !t.submitted);
        if (hasTaskAtPosition) {
            System.out.println("该位置已有建造任务，跳过: " + task.buildPosition);
            return;
        }
        taskList.add(task);
        System.out.println("添加建造任务: " + task.getIdempotentNo() + " - " + task.buildingType + " by worker " + task.worker.getID() + ", 当前任务数: " + taskList.size());
        // ✅ 如果工人携带资源，先让它返回基地放下
        if (task.worker.isCarryingMinerals() || task.worker.isCarryingGas()) {
            System.out.println("[DEBUG] 工人携带资源，先返回基地放下");
            Actions.returnCargo(task.worker);
            return;
        }
        // ✅ 使用 Actions 智能移动
        task.worker.stop();
        Actions.smartMove(task.worker, task.buildPosition);
    }

    public static int getCountByBuildingType(UnitType type) {
        synchronized (lock) {
            return (int) taskList.stream().filter(e -> e.buildingType == type).count();
        }
    }

    public static void consume() {
        synchronized (lock) {
            if (CollectionUtils.isEmpty(taskList)) {
                return;
            }
            // 创建副本进行遍历，避免并发修改
            List<BuildTask> snapshot = new LinkedList<>(taskList);
            snapshot.forEach(Builds::processTask);
        }
    }

    private static void processTask(BuildTask task) {
        if (task.isExpired()) {
            System.out.println("建造任务超时取消");
            removeTask(task);
            return;
        }
        if (task.submitted) {
            checkBuildCompletion(task);
            return;
        }
        // ✅ 如果工人还在携带资源，等待它放下
        if (task.worker.isCarryingMinerals() || task.worker.isCarryingGas()) {
            return;
        }
        moveToBuildPosition(task);
        if (canBuild(task)) {
            tryBuild(task);
        } else if (task.worker.isIdle()) {
            Actions.smartMove(task.worker, task.buildPosition);
        }
    }

    private static void moveToBuildPosition(BuildTask task) {
        int distance = task.worker.getDistance(task.buildPosition.toPosition());

        // ✅ 检测是否卡住：距离很近但无法建造
        if (distance <= 8 && !canBuild(task)) {
            // ✅ 可能是被其他单位临时挡住，强制重新移动
            if (task.worker.isIdle()) {
                System.out.println("[DEBUG] 工人可能卡住，强制重新移动: " + task.getIdempotentNo());
                Actions.forceMove(task.worker, task.buildPosition.toPosition());
            }
            return;
        }

        // 距离较远时，定期重新发送移动命令
        if (distance > 8 && task.worker.isIdle()) {
            Actions.smartMove(task.worker, task.buildPosition);
        }
    }


    private static boolean canBuild(BuildTask task) {
        // ✅ 先检查资源是否足够
        if (task.worker.getPlayer().minerals() < task.buildingType.mineralPrice()
                || task.worker.getPlayer().gas() < task.buildingType.gasPrice()) {
            return false;
        }

        // ✅ 使用 TilePosition 直接判断格子距离，而不是像素距离
        TilePosition workerTile = task.worker.getTilePosition();
        int tileDistanceX = Math.abs(workerTile.getX() - task.buildPosition.getX());
        int tileDistanceY = Math.abs(workerTile.getY() - task.buildPosition.getY());

        // 人族建筑：工人在建筑周围 2 格内即可建造
        return tileDistanceX <= 2 && tileDistanceY <= 2;
    }

    private static void tryBuild(BuildTask task) {
        System.out.println("建造" + task.buildingType + "，工人空闲：" + task.worker.isIdle()
                + "，位置: " + task.worker.getTilePosition() + " -> " + task.buildPosition);
        // ✅ 堵口任务使用宽松验证（只检查地图边界和地形）
        boolean isWallOff = task.getIdempotentNo().startsWith("first") ||
                task.getIdempotentNo().contains("bunker") ||
                task.getIdempotentNo().contains("wall");
        if (isWallOff) {
            // 堵口建筑：只检查基本可建造性
            if (!Games.isBuildable(task.buildPosition)) {
                System.out.println("[ERROR] 堵口位置地形不可建造");
                Positions.markFailedPosition(task.buildPosition);
                task.worker.stop();
                removeTask(task);
                return;
            }
        } else {
            // 普通建筑：使用完整验证
            if (!LocationValidator.isValid(task.buildPosition, task.buildingType)) {
                System.out.println("[ERROR] 位置验证失败");
                Positions.markFailedPosition(task.buildPosition);
                task.worker.stop();
                removeTask(task);
                return;
            }
        }

        if (task.worker.build(task.buildingType, task.buildPosition)) {
            System.out.println("成功下达建造命令");
            task.submitted = true;
        } else {
            System.out.println("建造命令失败，放弃任务...");

            TilePosition workerTile = task.worker.getTilePosition();
            int tileDistX = Math.abs(workerTile.getX() - task.buildPosition.getX());
            int tileDistY = Math.abs(workerTile.getY() - task.buildPosition.getY());

            System.out.println("[DEBUG] 工人位置: " + workerTile
                    + ", 目标位置: " + task.buildPosition
                    + ", 格子距离: (" + tileDistX + ", " + tileDistY + ")"
                    + ", 资源: " + task.worker.getPlayer().minerals() + "/" + task.buildingType.mineralPrice());

            Positions.markFailedPosition(task.buildPosition);

            task.worker.stop();
            removeTask(task);
        }
    }


    public static List<Unit> getBuildingWorkers() {
        return taskList.stream().map(e -> e.worker).collect(Collectors.toList());
    }

    private static void removeTask(BuildTask task) {
        taskList.remove(task);
        System.out.println("移除建造任务: " + task.getIdempotentNo() + ", 剩余任务数: " + taskList.size());
    }

    private static Unit findBuildingAtPosition(TilePosition position, UnitType type) {
        return Games.game.getAllUnits().stream()
                .filter(u -> u.getType() == type)
                .filter(u -> {
                    TilePosition unitPos = u.getTilePosition();
                    return Math.abs(unitPos.getX() - position.getX()) <= 1
                            && Math.abs(unitPos.getY() - position.getY()) <= 1;
                })
                .findFirst()
                .orElse(null);
    }

    private static void checkBuildCompletion(BuildTask task) {
        // ✅ 直接查找建筑，根据建筑状态判断
        Unit building = findBuildingAtPosition(task.buildPosition, task.buildingType);
        if (building == null) {
            // 建筑不存在
            if (task.worker.isConstructing()) {
                // 工人在建造但找不到建筑，可能是刚放下还没生成，等待下一帧
                System.out.println("[DEBUG] 工人正在建造，但建筑未生成，等待...");
                return;
            }
            // 工人也不在建造，可能建造失败
            System.out.println("建筑不存在且工人未建造，重试...");
            task.worker.stop();
            task.submitted = false;
            return;
        }

        // 建筑已完成
        if (building.isCompleted()) {
            System.out.println("建筑已完成: " + task.getIdempotentNo());
            // 执行回调（如果有）
            if (Objects.nonNull(task.callback)) {
                System.out.println("执行回调: " + task.getIdempotentNo());
                task.callback.execute();
            }
            // 移除任务
            removeTask(task);
            return;
        }

        // 建筑正在建造中
        if (building.isBeingConstructed()) {
            return;
        }

        // 建筑存在但未开始建造（可能是被打断）
        System.out.println("建筑存在但未建造，重试...");
        if (building.getPlayer().getRace() == Race.Terran) {
            retryWithNewWorker(task);
        } else {
            task.worker.stop();
            task.submitted = false;
        }
    }

    private static void retryWithNewWorker(BuildTask task) {
        System.out.println("人族建造被打断，重新分配工人继续建造");
        Unit newWorker = Workers.getAWorker(task.worker.getPlayer(), task.buildPosition.toPosition());

        if (newWorker != null) {
            task.worker.stop();
            task.worker = newWorker;
            task.submitted = false;
            System.out.println("已分配新工人 ID: " + newWorker.getID());
        } else {
            System.out.println("没有可用工人，等待下次重试");
        }
    }

    /**
     * 清理所有建造任务
     */
    public static void clearAll() {
        synchronized (lock) {
            taskList.clear();
        }
    }

    public static BuildTask getTaskByIdempotent(String idempotentNo) {
        return taskList.stream()
                .filter(t -> t.getIdempotentNo().equals(idempotentNo))
                .findFirst()
                .orElse(null);
    }

}
