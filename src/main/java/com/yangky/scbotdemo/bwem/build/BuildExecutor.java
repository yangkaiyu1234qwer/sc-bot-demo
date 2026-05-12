package com.yangky.scbotdemo.bwem.build;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.build.handler.ConstructingHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * BuildExecutor
 *
 * @author yangky
 * @Date 2026/5/9 22:45
 */
public class BuildExecutor {
    private static final TreeSet<Task> tasks = new TreeSet<>();
    private static final Set<StateHandler> handlerList = new HashSet<>();

    public static void clear() {
        tasks.clear();
    }

    public static void handleRegister(StateHandler handler) {
        handlerList.add(handler);
    }

    public static synchronized void add(Task task) {
        System.out.println("[DEBUG] 尝试添加任务: " + task.getIdempotentNo() + ", 当前任务数: " + tasks.stream().filter(e -> e.getStatus() != TaskStatus.COMPLETED).count());
        // ✅ 幂等检查
        if (tasks.stream().anyMatch(e -> StringUtils.equals(e.getIdempotentNo(), task.getIdempotentNo()))) {
            System.out.println("任务已存在，跳过: " + task.getIdempotentNo());
            return;
        }
        tasks.add(task);
    }

    public static void execute() {
        tasks.stream()
                .filter(e -> e.getStatus() != TaskStatus.COMPLETED).forEach(e -> {
                    handlerList.forEach(f -> {
                        try {
                            if (f.accessStatus() == e.getStatus()) {
                                if (e.isExpired()) {
                                    e.setStatus(TaskStatus.FAILED);
                                }
                                f.process(e);
                            }
                        } catch (Exception ex) {
                            System.out.println("[ERROR] 处理任务时发生异常: " + e.getIdempotentNo() + ", 错误: " + ex.getMessage());
                            ex.printStackTrace();
                            e.setStatus(TaskStatus.FAILED);
                        }
                    });
                });
        tasks.removeIf(e -> e.getStatus() == TaskStatus.FAILED);
    }

    public static int getCountByBuildingType(UnitType type) {
        return (int) tasks.stream()
                .filter(e -> e.getBuildingType() == type)
                .filter(e -> e.getStatus() != TaskStatus.COMPLETED /*&& e.getStatus() != TaskStatus.FAILED*/)  // ✅ 只统计未完成的任务
                .count();
    }

    public static Task getTaskByIdempotent(String idempotentNo) {
        return tasks.stream().filter(e -> StringUtils.equals(e.getIdempotentNo(), idempotentNo)).findFirst().orElse(null);
    }

    public static Task findTaskByPositionAndType(TilePosition position, UnitType type) {
        int searchRadius = Math.max(type.tileWidth(), type.tileHeight()) + 1;
        return tasks.stream()
                .filter(e -> e.getStatus() == TaskStatus.CONSTRUCTING || e.getStatus() == TaskStatus.COMPLETED)
                .filter(t -> {
                    TilePosition taskPos = t.getPosition();
                    if (taskPos == null) return false;

                    return Math.abs(taskPos.getX() - position.getX()) <= searchRadius &&
                            Math.abs(taskPos.getY() - position.getY()) <= searchRadius &&
                            t.getBuildingType() == type;
                })
                .findFirst()
                .orElse(null);
    }

    public static void onUnitComplete(Unit unit) {
        ConstructingHandler.onUnitComplete(unit);
    }

    public static boolean isOnTask(Unit unit) {
        return tasks.stream()
                .filter(e -> e.getStatus() != TaskStatus.COMPLETED && e.getStatus() != TaskStatus.FAILED)
                .anyMatch(e -> e.getWorker() == unit);
    }
}
