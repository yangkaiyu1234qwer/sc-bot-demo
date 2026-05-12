package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.TilePosition;
import bwapi.Unit;
import com.yangky.scbotdemo.bwem.Actions;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * BuildingHandler
 *
 * @author yangky
 * @Date 2026/5/10 8:58
 */
@Component
public class PreBuildHandler extends StateHandler {
    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.PRE_BUILD;
    }

    @Override
    public void process(Task task) {
        if (checkBuildConstructing(task)) {
            return;
        }
        // 先检查资源是否足够
        if (task.getWorker().getPlayer().minerals() < task.getBuildingType().mineralPrice()
                || task.getWorker().getPlayer().gas() < task.getBuildingType().gasPrice()) {
            return;
        }
        TilePosition workerTile = task.getWorker().getTilePosition();
        int tileDistanceX = Math.abs(workerTile.getX() - task.getPosition().getX());
        int tileDistanceY = Math.abs(workerTile.getY() - task.getPosition().getY());
        if (tileDistanceX > 2 && tileDistanceY > 2) {
            Actions.smartMove(task.getWorker(), task.getPosition());
            return;
        }
        // 堵口任务使用宽松验证
        boolean isWallOff = task.getIdempotentNo().startsWith("first") ||
                task.getIdempotentNo().contains("bunker") ||
                task.getIdempotentNo().contains("barracks") ||
                task.getIdempotentNo().contains("wall");
        if (isWallOff) {
            if (!Games.isBuildable(task.getPosition())) {
                System.out.println("[ERROR] 堵口位置地形不可建造" + task.getPosition() + ", buildingType=" + task.getBuildingType());
                BuildingPlacer.markFailedPosition(task.getPosition());
                task.getWorker().stop();
                task.setStatus(TaskStatus.FAILED);
                return;
            }
        } else {
            if (!LocationValidator.isValid(task.getPosition(), task)) {
                System.out.println("[ERROR] 位置验证失败 buildPosition=" + task.getPosition() + ", buildingType=" + task.getBuildingType());
                BuildingPlacer.markFailedPosition(task.getPosition());
                task.getWorker().stop();
                task.setStatus(TaskStatus.FAILED);
                return;
            }
        }
        if (task.getStatus() != TaskStatus.CONSTRUCTING && !task.getWorker().build(task.getBuildingType(), task.getPosition())) {
            System.out.println("[ERROR] 下达建造命令失败" + task.getPosition() + ", buildingType=" + task.getBuildingType());
        } else {
            System.out.println("成功下达建造命令,等待轮询确认" + task.getPosition() + ", buildingType=" + task.getBuildingType());
        }
    }

    public boolean checkBuildConstructing(Task task) {
        Unit building = Units.findBuildingAtPosition(task.getPosition(), task.getBuildingType());
        if (building != null) {
            task.setStatus(TaskStatus.CONSTRUCTING);
        }
        return building != null;
    }
}
