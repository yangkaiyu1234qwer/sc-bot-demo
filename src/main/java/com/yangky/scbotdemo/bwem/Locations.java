package com.yangky.scbotdemo.bwem;

import bwapi.TilePosition;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.bwem.region.RegionsClassifier;

import java.util.Set;
import java.util.stream.Collectors;

public class Locations {
    private static boolean initialized = false;


    /**
     * 初始化地图区域分析
     */
    public static void initialize() {
        RegionsClassifier.initialize();
        initialized = true;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取指定区域类型的所有位置
     */
    public static Set<TilePosition> getPositionsByRegion(RegionType type) {
        return RegionsClassifier.getPositions()
                .stream()
                .filter(e -> e.getType() == type && e.isBuildable())
                .map(e -> new TilePosition(e.getX(), e.getY()))
                .collect(Collectors.toSet());
    }

    public static TilePosition getCentralAreaCenter() {
        return RegionsClassifier.getCentralPosition();
    }
}
