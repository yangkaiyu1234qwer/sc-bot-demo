package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.TilePosition;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.util.Positions;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScvAssignedHandler
 *
 * @author yangky
 * @Date 2026/5/10 8:23
 */
@Component
public class AssignPositionHandler extends StateHandler {
    @Getter
    private static final Map<TilePosition, Task> positionCache = new ConcurrentHashMap<>();

    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.ASSIGN_POSITION;
    }

    @Override
    public void process(Task task) {
        synchronized (AssignPositionHandler.class) {
            if (task.getPosition() == null) {
                TilePosition pos = null;
                if (task.getBuildingType() == UnitType.Terran_Supply_Depot
                        || task.getBuildingType() == UnitType.Terran_Engineering_Bay) {
                    List<RegionType> regionTypeList = new ArrayList<>();
                    regionTypeList.add(RegionType.BOUNDARY);
                    regionTypeList.add(RegionType.EDGE);
                    pos = BuildingPlacer.findPosition(task.getBuildingType(), regionTypeList, 0, 0, true);
//                task.setPosition(Positions.getEdgePosition(task.getBuildingType(), Bases.getMainBaseUnit()));
                } else if (task.getBuildingType() == UnitType.Terran_Missile_Turret) {
                    List<RegionType> regionTypeList = new ArrayList<>();
                    regionTypeList.add(RegionType.BOUNDARY);
                    regionTypeList.add(RegionType.EDGE);
                    regionTypeList.add(RegionType.MINERAL);
                    pos = BuildingPlacer.findPosition(task.getBuildingType(), regionTypeList, 0, 0, false);
                } else {
//                task.setPosition(Positions.getCentralPosition(task.getBuildingType(), Bases.getMainBase().getLocation()));
                    List<RegionType> regionTypeList = new ArrayList<>();
                    regionTypeList.add(RegionType.CENTRAL);
                    pos = BuildingPlacer.findPosition(task.getBuildingType(), regionTypeList, 0, 0, true);
                }
                task.setPosition(pos);
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
            positionCache.put(task.getPosition(), task);
            task.setStatus(TaskStatus.MOVING);
        }
    }

    public static synchronized void checkAndReleasePosition() {
        positionCache.forEach((pos, task) -> {
            if (task.getStatus() == TaskStatus.FAILED) {
                positionCache.remove(pos);
                return;
            }
            if (task.getStatus() == TaskStatus.COMPLETED) {
                if (Units.findBuildingAtPosition(pos, null) == null) {
                    System.out.println("[DEBUG] 任务完成但位置上没有建筑，释放位置: " + pos);
                    positionCache.remove(pos);
                }
            }
        });
    }

    public void clear() {
        positionCache.clear();
    }
}
