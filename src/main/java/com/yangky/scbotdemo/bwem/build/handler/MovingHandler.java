package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Actions;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class MovingHandler extends StateHandler {
//    private final Map<Task, LinkedHashMap<Long, TilePosition>> scvMoveHistory = new HashMap<>();

    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.MOVING;
    }

    @Override
    public void process(Task task) {
        // 使用 TilePosition 直接判断格子距离
        TilePosition workerTile = task.getWorker().getTilePosition();
        int tileDistanceX = Math.abs(workerTile.getX() - task.getPosition().getX());
        int tileDistanceY = Math.abs(workerTile.getY() - task.getPosition().getY());
        int tileDistanceLimit = task.getBuildingType() == UnitType.Terran_Refinery ? 2 : 1;
        if (tileDistanceX <= tileDistanceLimit && tileDistanceY <= tileDistanceLimit) {
            // 已到达，修改状态去建造
            task.setStatus(TaskStatus.PRE_BUILD);
        } else {
            // 未到达，校验目标位置是否被占用
            Unit conflict = LocationValidator.buildingConflict(task.getPosition(), task);
            if (conflict != null) {
                task.setStatus(TaskStatus.ASSIGN_POSITION);
                return;
            } else {
                Task conflict1 = LocationValidator.taskConflict(task);
                if (conflict1 != null) {
                    task.setStatus(TaskStatus.ASSIGN_POSITION);
                    return;
                }
            }
        }
        Actions.smartMove(task.getWorker(), task.getPosition());
    }
}
