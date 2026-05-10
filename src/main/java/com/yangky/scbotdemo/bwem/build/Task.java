package com.yangky.scbotdemo.bwem.build;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.Callback;
import lombok.Data;

/**
 * Task
 *
 * @author yangky
 * @Date 2026/5/9 21:52
 */
@Data
public class Task implements Comparable<Task> {
    private String idempotentNo;
    private TilePosition position;
    private Unit worker;
    private UnitType buildingType;
    private long startTime;
    private TaskStatus status;
    private int retryCount;
    private int priority;
    public Callback callback;

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime >= 45000;
    }

    public boolean isRetryLimited() {
        return retryCount >= 3;
    }

    @Override
    public int compareTo(Task o) {
        int statusCompare = Integer.compare(this.status.getCode(), o.getStatus().getCode());
        int priorityCompare = Integer.compare(this.priority, o.priority);
        int timeCompare = Long.compare(this.startTime, o.startTime);
        return statusCompare * 100 + priorityCompare * 10 + timeCompare;
    }

    public static Task ofSupplyDepot(String idempotentNo, TilePosition position) {
        Task task = new Task();
        task.setIdempotentNo(idempotentNo);
        task.setPosition(position);
        task.setBuildingType(UnitType.Terran_Supply_Depot);
        task.setStartTime(System.currentTimeMillis());
        task.setStartTime(System.currentTimeMillis());
        task.setStatus(TaskStatus.WAITING);
        task.setPriority(1);
        return task;
    }

    public static Task of(String idempotentNo, TilePosition position, UnitType buildingType) {
        Task task = new Task();
        task.setIdempotentNo(idempotentNo);
        task.setPosition(position);
        task.setBuildingType(buildingType);
        task.setStartTime(System.currentTimeMillis());
        task.setStatus(TaskStatus.WAITING);
        task.setPriority(3);
        return task;
    }

    public static Task of(String idempotentNo, TilePosition position, UnitType buildingType, Callback callback) {
        Task task = new Task();
        task.setIdempotentNo(idempotentNo);
        task.setPosition(position);
        task.setBuildingType(buildingType);
        task.setStartTime(System.currentTimeMillis());
        task.setStatus(TaskStatus.WAITING);
        task.setPriority(3);
        task.setCallback(callback);
        return task;
    }
}
