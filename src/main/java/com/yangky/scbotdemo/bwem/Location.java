package com.yangky.scbotdemo.bwem;

import bwapi.TilePosition;
import bwem.ChokePoint;
import lombok.Data;
import lombok.Getter;

/**
 * Location
 *
 * @author yangky
 * @Date 2026/5/2 12:55
 */
@Data
public class Location {
    private TilePosition tilePosition;
    private RegionType regionType;
    private boolean isBuildable;
    private int distanceToBase;
    private int distanceToBoundary;
    private double distanceToNearestMineral;
    private ChokePoint nearestChokePoint;

    public Location(TilePosition tilePosition, boolean isBuildable) {
        this.tilePosition = tilePosition;
        this.isBuildable = isBuildable;
        this.regionType = RegionType.UNKNOWN;
    }

    public boolean isEdgeZone() {
        return regionType == RegionType.EDGE;
    }

    public boolean isCentralZone() {
        return regionType == RegionType.CENTRAL;
    }

    public boolean isMineralZone() {
        return regionType == RegionType.MINERAL;
    }

    public boolean isChokePointZone() {
        return regionType == RegionType.CHOKE_POINT;
    }


    @Getter
    public enum RegionType {
        UNKNOWN,
        MINERAL,
        CHOKE_POINT,
        EDGE,
        CENTRAL,
        BOUNDARY
    }
}
