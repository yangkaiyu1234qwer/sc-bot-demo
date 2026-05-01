package com.yangky.scbotdemo.bwem.task;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.Callback;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * BuildTask
 *
 * @author yangky
 * @Date 2026/4/28 8:55
 */
public class BuildTask {
    // 幂等编号
    @Getter
    private final String idempotentNo;
    public Unit worker;
    public TilePosition buildPosition;
    public UnitType buildingType;
    public long startTime;
    public boolean submitted = false;
    public Callback callback;

    public Boolean isTheSame(BuildTask task) {
        return StringUtils.equals(task.idempotentNo, idempotentNo);
    }

    public BuildTask(String idempotentNo, TilePosition buildPosition, UnitType buildingType) {
        this.idempotentNo = idempotentNo;
        this.buildPosition = buildPosition;
        this.buildingType = buildingType;
        this.startTime = System.currentTimeMillis();
    }

    public BuildTask(String idempotentNo, TilePosition buildPosition, UnitType buildingType, Callback callback) {
        this.idempotentNo = idempotentNo;
        this.buildPosition = buildPosition;
        this.buildingType = buildingType;
        this.callback = callback;
        this.startTime = System.currentTimeMillis();
    }

    public BuildTask(String idempotentNo, Unit worker, TilePosition buildPosition, UnitType buildingType, Callback callback) {
        this.idempotentNo = idempotentNo;
        this.worker = worker;
        this.buildPosition = buildPosition;
        this.buildingType = buildingType;
        this.startTime = System.currentTimeMillis();
        this.callback = callback;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime >= 120000;
    }
}
