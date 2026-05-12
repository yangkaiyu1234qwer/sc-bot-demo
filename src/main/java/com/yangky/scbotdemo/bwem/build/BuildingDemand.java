package com.yangky.scbotdemo.bwem.build;

import bwapi.TilePosition;
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
    private boolean allowBWAPIFallback;
    private TilePosition position;

    public BuildingDemand() {
    }

    public static BuildingDemand of(UnitType unitType, int i, RegionStrategy strategy) {
        BuildingDemand demand = new BuildingDemand();
        demand.setBuildingType(unitType);
        demand.setTargetCount(i);
        demand.setRegionStrategy(strategy);
        return demand;
    }

    public static BuildingDemand of(UnitType unitType, int i, RegionStrategy strategy, int xOffset, int yOffset, boolean allowBWAPIFallback) {
        BuildingDemand demand = new BuildingDemand();
        demand.setBuildingType(unitType);
        demand.setTargetCount(i);
        demand.setRegionStrategy(strategy);
        demand.setXOffset(xOffset);
        demand.setYOffset(yOffset);
        demand.setAllowBWAPIFallback(allowBWAPIFallback);
        return demand;
    }

    public static BuildingDemand of(UnitType unitType, int i, TilePosition position) {
        BuildingDemand demand = new BuildingDemand();
        demand.setBuildingType(unitType);
        demand.setTargetCount(i);
        demand.setPosition(position);
        return demand;
    }
}
