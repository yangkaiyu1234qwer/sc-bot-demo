package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.TilePosition;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import com.yangky.scbotdemo.util.Positions;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScvAssignedHandler
 *
 * @author yangky
 * @Date 2026/5/10 8:23
 */
@Component
public class AssignPositionHandler extends StateHandler {

    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.ASSIGN_POSITION;
    }

    @Override
    public void process(Task task) {
        if (task.getPosition() == null) {
            if (task.getBuildingType() == UnitType.Terran_Supply_Depot || task.getBuildingType() == UnitType.Terran_Missile_Turret
                    || task.getBuildingType() == UnitType.Terran_Engineering_Bay) {
                task.setPosition(Positions.getEdgePosition(task.getBuildingType(), Bases.getMainBaseUnit()));
            } else {
                task.setPosition(Positions.getCentralPosition(task.getBuildingType(), Bases.getMainBase().getLocation()));
            }
        } else if (!LocationValidator.isValid(task.getPosition(), task.getBuildingType())) {
            if (Units.findBuildingAtPosition(task.getPosition(), null) != null) {
                System.out.println("[DEBUG] 位置上已存在建筑 task=" + task.getIdempotentNo() + ", pos=" + task.getPosition());
            } else {
                System.out.println("[DEBUG] 位置不可建造 task=" + task.getIdempotentNo() + ", pos=" + task.getPosition());
            }
            Positions.markFailedPosition(task.getPosition());
            task.setPosition(null);
            task.setStatus(TaskStatus.RETRYING);
            return;
        }
        task.setStatus(TaskStatus.MOVING);
    }
}
