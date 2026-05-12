package com.yangky.scbotdemo.bwem.build;

import bwapi.UnitType;
import com.yangky.scbotdemo.BuildTiming;
import com.yangky.scbotdemo.Callback;
import lombok.Data;

/**
 * BuildingDemand
 *
 * @author yangky
 * @Date 2026/5/12 11:41
 */
@Data
public class BuildingDemand {
    private UnitType buildingType;
    private int targetCount;
    private RegionStrategy regionStrategy;  // 可选
    private BuildTiming buildTiming;        // 可选
    private Callback callback;              // 可选
    private int xOffset;
    private int yOffset;
    public BuildingDemand() {
    }

    public BuildingDemand(UnitType buildingType, int targetCount, RegionStrategy regionStrategy, BuildTiming buildTiming, Callback callback) {
        this.buildingType = buildingType;
        this.targetCount = targetCount;
        this.regionStrategy = regionStrategy;
        this.buildTiming = buildTiming;
        this.callback = callback;
    }

    public BuildingDemand(UnitType type, int count) {
        this.buildingType = type;
        this.targetCount = count;
    }

    /**
     * 构造完整需求
     * @param type 建筑类型
     * @param count 目标数量
     * @param strategy 选址策略（可选）
     * @param timing 建造时机（可选）
     * @param callback 完成回调（可选）
     * @param xOffset X方向间距扩展（可选，默认0）
     * @param yOffset Y方向间距扩展（可选，默认0）
     */
    public BuildingDemand(UnitType type, int count, RegionStrategy strategy,
                          BuildTiming timing, Callback callback, int xOffset, int yOffset) {
        this.buildingType = type;
        this.targetCount = count;
        this.regionStrategy = strategy;
        this.buildTiming = timing;
        this.callback = callback;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }
}
