package com.yangky.scbotdemo.bwem.build.handler;

import bwapi.TilePosition;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import lombok.Getter;
import org.springframework.stereotype.Component;

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
        boolean isWallOff = task.getIdempotentNo().startsWith("first") ||
                task.getIdempotentNo().contains("second") ||
                task.getIdempotentNo().contains("wall");
        synchronized (AssignPositionHandler.class) {
            if (task.getPosition() == null) {
                // 选址（如果有策略）
                TilePosition position = BuildingPlacer.findPosition(task);
                if (position == null) {
                    System.out.println("[DEBUG] 没有找到合适的位置 task=" + task.getIdempotentNo() + ", buildingType=" + task.getBuildingType());
                }
                task.setPosition(position);
            } else if (isWallOff && !Games.isBuildable(task.getPosition())) {
                System.out.println("[ERROR] 堵口位置地形不可建造" + task.getPosition() + ", buildingType=" + task.getBuildingType());
                BuildingPlacer.markFailedPosition(task.getPosition());
                task.getWorker().stop();
                task.setStatus(TaskStatus.FAILED);
                return;
            } else if (!LocationValidator.isValid(task.getPosition(), task)) {
                if (Units.findBuildingAtPosition(task.getPosition(), null) != null) {
                    System.out.println("[DEBUG] 位置上已存在建筑 task=" + task.getIdempotentNo() + ", pos=" + task.getPosition());
                } else {
                    System.out.println("[DEBUG] 位置不可建造 task=" + task.getIdempotentNo() + ", pos=" + task.getPosition());
                }
                BuildingPlacer.markFailedPosition(task.getPosition());
                task.setPosition(null);
                task.setStatus(TaskStatus.RETRYING);
                return;
            }
            positionCache.put(task.getPosition(), task);
            task.setStatus(TaskStatus.ASSIGN_SCV);
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
