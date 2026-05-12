package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.Order;
import bwapi.TilePosition;
import bwapi.Unit;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import com.yangky.scbotdemo.bwem.region.RegionsClassifier;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Random;

/**
 * BuildingHandler
 *
 * @author yangky
 * @Date 2026/5/10 8:58
 */
@Component
public class ConstructingHandler extends StateHandler {
    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.CONSTRUCTING;
    }

    @Override
    public void process(Task task) {
        // 直接查找建筑，根据建筑状态判断
        Unit building = Units.findBuildingAtPosition(task.getPosition(), task.getBuildingType());
        if (building == null || building.getPlayer() != Games.game.self()) {
            if (task.getWorker().isConstructing()) {
                // 工人在建造但找不到建筑，可能是刚放下还没生成，等待下一帧
                System.out.println("[DEBUG] 工人正在建造，但建筑未生成，等待...");
            } else {
                System.out.println("建筑不存在且工人未建造，重试...");
            }
            return;
        }
        // 建筑已完成
        if (building.isCompleted() && task.getStatus() != TaskStatus.COMPLETED) {
            onUnitComplete(building);
            return;
        }
        // 建筑存在但未建造（可能是被打断）
        if (building.getHitPoints() > 0) {
            // 建筑已经有血量，说明正在建造中
            // 检查当前任务的工人是否还在修
            if (task.getWorker() != null && task.getWorker().exists()) {
                Order order = task.getWorker().getOrder();
                if ((order == Order.ConstructingBuilding || order == Order.ResetCollision)
                        && (task.getWorker().getOrderTarget() != null && task.getWorker().getOrderTarget().equals(building))) {
                }
            } else {
                Unit worker = Workers.getAWorker(building.getPosition());
                if (worker != null) {
                    task.setWorker(worker);
                    worker.rightClick(building);
                    System.out.println("[DEBUG] 重新分配工人维修建筑: " + worker.getID());
                }
            }
        }
    }

    public static void onUnitComplete(Unit unit) {
        Task task = BuildExecutor.findTaskByPositionAndType(unit.getTilePosition(), unit.getType());
        if (task != null) {
            onTaskComplete(task);
        }
    }

    public static void onTaskComplete(Task task) {
        System.out.println("建筑已完成: " + task.getIdempotentNo());
        // 标记为有经验建筑师，并让它回到基地附近
        Workers.markAsExperiencedBuilder(task.getWorker());
        Unit worker = task.getWorker();
        if (worker.getOrder() == Order.ConstructingBuilding) {
            return;
        }
        // ✅ 清除卡住记录，避免误判
        Actions.clearWorkerTracking(worker);
        // ✅ 直接下达移动命令，不需要先 stop()
        TilePosition central = RegionsClassifier.getCentralPosition();
        int randomX = new Random().nextInt(10);
        int randomY = new Random().nextInt(10);
        TilePosition target = new TilePosition(central.getX() + randomX - 5, central.getY() + randomY - 5);
        worker.rightClick(target.toPosition());
        Workers.goGatherLessLoader(worker, Bases.getMainBaseUnit());
        System.out.println("建造完毕 task=" + task.getIdempotentNo() + ",  pos=" + target);
        // 执行回调（如果有）
        if (Objects.nonNull(task.callback)) {
            System.out.println("执行回调: " + task.getIdempotentNo());
            task.callback.execute();
        }
        task.setStatus(TaskStatus.COMPLETED);
    }

}
