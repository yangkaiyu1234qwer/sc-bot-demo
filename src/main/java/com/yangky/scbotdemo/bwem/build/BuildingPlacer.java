package com.yangky.scbotdemo.bwem.build;


import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.bwem.region.RegionsClassifier;
import com.yangky.scbotdemo.util.Positions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BuildingPlacer - 建筑选址器
 * 根据指定区域自动计算合适的建筑位置
 *
 * @author yangky
 * @Date 2026/5/10
 */
public class BuildingPlacer {

    private static final int DEFAULT_SEARCH_RADIUS = 30; // 默认搜索半径
    // 使用缓存记录最近提供的坐标，防止并发冲突，缓存有效时间15秒
    private static final Cache<TilePosition, TilePosition> cache = Caffeine.newBuilder()
            .maximumSize(15000)
            .expireAfterWrite(1, java.util.concurrent.TimeUnit.MINUTES)
            .build();

    /**
     * 在指定区域内寻找合适的建筑位置（使用默认参数）
     *
     * @param building   建筑类型
     * @param regionType 目标区域类型
     * @return 合适的 TilePosition，如果找不到返回 null
     */
    public static TilePosition findPosition(UnitType building, RegionType regionType) {
        return findPosition(building, regionType, 0, 0, null, true);
    }

    /**
     * 在指定区域内寻找合适的建筑位置（支持偏移量）
     *
     * @param building   建筑类型
     * @param regionType 目标区域类型
     * @param xOffset    X方向额外扩展的格子数（用于控制建筑间距）
     * @param yOffset    Y方向额外扩展的格子数（用于控制建筑间距）
     * @return 合适的 TilePosition，如果找不到返回 null
     */
    public static TilePosition findPosition(UnitType building, RegionType regionType, int xOffset, int yOffset) {
        return findPosition(building, regionType, xOffset, yOffset, null, true);
    }

    /**
     * 在指定区域内寻找合适的建筑位置（完整参数）
     *
     * @param building           建筑类型
     * @param regionType         目标区域类型
     * @param xOffset            X方向额外扩展的格子数（用于控制建筑间距）
     * @param yOffset            Y方向额外扩展的格子数（用于控制建筑间距）
     * @param fallbackRegions    备选区域列表（主区域找不到时尝试）
     * @param allowBWAPIFallback 是否允许降级到 BWAPI 随机选择
     * @return 合适的 TilePosition，如果找不到返回 null
     */
    public static TilePosition findPosition(UnitType building, RegionType regionType,
                                            int xOffset, int yOffset,
                                            List<RegionType> fallbackRegions,
                                            boolean allowBWAPIFallback) {
        if (building == null || regionType == null || !Locations.isInitialized()) {
            return null;
        }
        System.out.println("[BuildingPlacer] 开始选址 - 建筑: " + building
                + ", 主区域: " + regionType
                + ", 偏移量: (" + xOffset + ", " + yOffset + ")"
                + ", 备选区域: " + (fallbackRegions != null ? fallbackRegions.size() : 0) + " 个"
                + ", 允许BWAPI降级: " + allowBWAPIFallback);
        // 构建区域优先级列表（主区域 + 备选区域）
        List<RegionType> regionPriority = new ArrayList<>();
        regionPriority.add(regionType);
        if (fallbackRegions != null && !fallbackRegions.isEmpty()) {
            regionPriority.addAll(fallbackRegions);
        }
        TilePosition result = null;
        // 按优先级尝试各个区域
        for (RegionType targetRegion : regionPriority) {
            result = tryPlaceInRegion(building, targetRegion, xOffset, yOffset);
            if (result != null) {
                System.out.println("[BuildingPlacer] ✓ 在区域 " + targetRegion + " 找到位置: " + result);
            } else {
                System.out.println("[BuildingPlacer] ✗ 在区域 " + targetRegion + " 未找到位置");
            }
        }
        // 所有区域都失败，考虑是否使用 BWAPI 兜底
        if (result == null && allowBWAPIFallback) {
            TilePosition fallback = Games.game.getBuildLocation(building, Bases.getMainBase().getLocation(), DEFAULT_SEARCH_RADIUS);
            if (fallback != null) {
                System.out.println("[BuildingPlacer] ⚠ 使用 BWAPI 兜底位置: " + fallback);
                result = fallback;
            }
        }
        if (result == null) {
            System.out.println("[BuildingPlacer] ✗ 所有方案均失败，返回 null");
        } else {
            cache.put(result, result);
        }
        return result;
    }

    /**
     * 在指定区域内寻找合适的建筑位置（多区域优先级，支持偏移量）
     *
     * @param building           建筑类型
     * @param preferredRegions   优先区域列表（按优先级排序）
     * @param xOffset            X方向额外扩展的格子数
     * @param yOffset            Y方向额外扩展的格子数
     * @param allowBWAPIFallback 是否允许降级到 BWAPI
     * @return 合适的 TilePosition，如果找不到返回 null
     */
    public static TilePosition findPosition(UnitType building, List<RegionType> preferredRegions,
                                            int xOffset, int yOffset, boolean allowBWAPIFallback) {
        if (preferredRegions == null || preferredRegions.isEmpty()) {
            return null;
        }
        RegionType primaryRegion = preferredRegions.get(0);
        List<RegionType> fallbackRegions = preferredRegions.size() > 1 ? preferredRegions.subList(1, preferredRegions.size()) : null;
        return findPosition(building, primaryRegion, xOffset, yOffset, fallbackRegions, allowBWAPIFallback);
    }

    /**
     * 尝试在指定区域内放置建筑
     *
     * @param building   建筑类型
     * @param regionType 区域类型
     * @param xOffset    X方向偏移
     * @param yOffset    Y方向偏移
     * @return 合适的 TilePosition
     */
    private static TilePosition tryPlaceInRegion(UnitType building, RegionType regionType, int xOffset, int yOffset) {
        // 获取区域内的所有可建造位置
        Set<TilePosition> regionPositions = Locations.getPositionsByRegion(regionType);
        if (regionPositions.isEmpty()) {
            return null;
        }
        // 计算区域中心点
        TilePosition center = null;
        if (regionType == RegionType.CENTRAL) {
            center = RegionsClassifier.getCentralPosition();
        } else if (regionType == RegionType.CHOKE_POINT) {
            Unit bunker = Units.getSelfUnits(UnitType.Terran_Bunker).stream().findFirst().orElse(null);
            if (bunker == null) {
                center = RegionsClassifier.getChokePointPosition();
            } else {
                center = new TilePosition(bunker.getTilePosition().getX(), bunker.getTilePosition().getY());
            }
        }
        // 确定起始搜索点
        if (center == null) {
            List<TilePosition> list = RegionsClassifier.getPositions().stream()
                    .map(e -> new TilePosition(e.getX(), e.getY()))
                    .collect(Collectors.toList());
            int random = (int) (Math.random() * list.size());
            center = list.get(random);
            if (center == null) {
                center = getRandomEdgePosition(regionPositions);
            }
            if (center == null) {
                center = Positions.getEdgePosition(building, Bases.getMainBaseUnit());
            }
        }
        // 执行 BFS 搜索
        return bfsSearch(building, center, regionPositions, xOffset, yOffset);
    }

    /**
     * BFS 搜索合适的建筑位置
     *
     * @param building        建筑类型
     * @param startPos        起始搜索点
     * @param regionPositions 区域内的所有位置集合
     * @param xOffset         X方向额外扩展
     * @param yOffset         Y方向额外扩展
     * @return 合适的 TilePosition
     */
    private static TilePosition bfsSearch(UnitType building, TilePosition startPos,
                                          Set<TilePosition> regionPositions,
                                          int xOffset, int yOffset) {
        int buildWidth = building.tileWidth() + xOffset;
        int buildHeight = building.tileHeight() + yOffset;

        // 记录已访问的位置
        Set<TilePosition> visited = new HashSet<>();
        Queue<TilePosition> queue = new LinkedList<>();

        queue.offer(startPos);
        visited.add(startPos);

        // 四个方向：上、下、左、右
        int[][] directions = new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            // 检查当前位置是否可以放下建筑（考虑偏移量）
            if (canPlaceBuilding(current, buildWidth, buildHeight, regionPositions, building) && !cache.asMap().containsKey(current)) {
                return current;
            }
            // 向四个方向扩展
            for (int[] dir : directions) {
                TilePosition next = new TilePosition(
                        current.getX() + dir[0],
                        current.getY() + dir[1]
                );
                // 跳过已访问的位置
                if (visited.contains(next)) {
                    continue;
                }
                // 如果下一个位置不在区域内，该方向停止扩展
                if (!regionPositions.contains(next)) {
                    continue;
                }
                visited.add(next);
                queue.offer(next);
            }
        }
        return null;
    }

    private static boolean canPlaceBuilding(TilePosition pos, int requiredWidth, int requiredHeight,
                                            Set<TilePosition> regionPositions, UnitType building) {
        // ✅ 先验证起始位置是否合法（使用 LocationValidator）
        if (!LocationValidator.isValid(pos, building)) {
            return false;
        }

        // 检查建筑占据的所有格子（包括偏移量）是否在区域内且可建造
        for (int dx = 0; dx < requiredWidth; dx++) {
            for (int dy = 0; dy < requiredHeight; dy++) {
                TilePosition tile = new TilePosition(pos.getX() + dx, pos.getY() + dy);

                // 检查是否在区域内
                if (!regionPositions.contains(tile)) {
                    return false;
                }

                // 检查是否可建造（只检查地形，不重复调用 isValid）
                if (!Games.isBuildable(tile)) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 从边缘区域随机选择一个位置
     *
     * @param positions 区域位置集合
     * @return 随机选择的 TilePosition
     */
    private static TilePosition getRandomEdgePosition(Set<TilePosition> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        List<TilePosition> edgePositions = new ArrayList<>();
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        int edgeThreshold = 3;

        for (TilePosition pos : positions) {
            if (pos.getX() <= edgeThreshold ||
                    pos.getY() <= edgeThreshold ||
                    pos.getX() >= mapWidth - edgeThreshold ||
                    pos.getY() >= mapHeight - edgeThreshold) {
                edgePositions.add(pos);
            }
        }
        if (edgePositions.isEmpty()) {
            // 如果没有真正的边缘位置，随机返回一个
            List<TilePosition> allPositions = new ArrayList<>(positions);
            return allPositions.get(new Random().nextInt(allPositions.size()));
        }
        return edgePositions.get(new Random().nextInt(edgePositions.size()));
    }
}
