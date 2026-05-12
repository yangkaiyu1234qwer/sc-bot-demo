package com.yangky.scbotdemo.bwem.build;


import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.build.handler.AssignPositionHandler;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.bwem.region.RegionsClassifier;

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
    private static final Set<String> failedPositions = new HashSet<>(); // 记录已尝试过但失败的位置


    public static synchronized TilePosition findPosition(Task task) {
        UnitType building = task.getBuildingType();
        RegionStrategy strategy = task.getRegionStrategy();
        if (building == null || strategy == null || strategy.getRegions() == null || strategy.getRegions().isEmpty() || !Locations.isInitialized()) {
            return null;
        }
        System.out.println("[BuildingPlacer] 开始选址 "
                + ", task=" + task.getIdempotentNo()
                + ", 区域: " + strategy.getRegions()
                + ", 偏移量: (" + task.getXOffset() + ", " + task.getYOffset() + ")"
                + ", 允许BWAPI降级: " + task.isAllowBWAPIFallback());
        TilePosition result = null;
        // 按优先级尝试各个区域
        for (RegionType targetRegion : task.getRegionStrategy().getRegions()) {
            result = tryPlaceInRegion(task, targetRegion);
            if (result != null) {
                System.out.println("[BuildingPlacer] ✓ 在区域 " + targetRegion + " 找到位置: " + result + ", task=" + task.getIdempotentNo());
            } else {
                System.out.println("[BuildingPlacer] ✗ 在区域 " + targetRegion + " 未找到位置" + ", task=" + task.getIdempotentNo());
            }
        }
        // 所有区域都失败，考虑是否使用 BWAPI 兜底
        if (result == null && task.isAllowBWAPIFallback()) {
            TilePosition fallback = Games.game.getBuildLocation(building, Bases.getMainBase().getLocation(), DEFAULT_SEARCH_RADIUS);
            if (fallback != null) {
                System.out.println("[BuildingPlacer] ⚠ 使用 BWAPI 兜底位置: " + fallback + ", task=" + task.getIdempotentNo());
                result = fallback;
            }
        }
        if (result == null) {
            System.out.println("[BuildingPlacer] ✗ 所有方案均失败，返回 null" + ", task=" + task.getIdempotentNo());
        }
        return result;
    }

    private static synchronized TilePosition tryPlaceInRegion(Task task, RegionType regionType) {
        Set<TilePosition> regionPositions = Locations.getPositionsByRegion(regionType);
        if (regionPositions.isEmpty()) {
            return null;
        }
        regionPositions = regionPositions.stream().filter(pos -> !AssignPositionHandler.getPositionCache().containsKey(pos)).collect(Collectors.toSet());
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
        if (center == null) {
            center = Bases.getMainBaseUnit().getTilePosition();
        }
        if (regionType == RegionType.EDGE || regionType == RegionType.BOUNDARY) {
            return directScanForEdgeOrMineral(task, regionPositions, center);
        }
        return bfsSearch(task, center, regionPositions);
    }

    private static TilePosition directScanForEdgeOrMineral(Task task,
                                                           Set<TilePosition> regionPositions,
                                                           TilePosition referencePoint) {
        int buildWidth = task.getBuildingType().tileWidth() + task.getXOffset();
        int buildHeight = task.getBuildingType().tileHeight() + task.getYOffset();
        List<TilePosition> validCandidates = new ArrayList<>();
        for (TilePosition pos : regionPositions) {
            if (canPlaceBuilding(pos, task, buildWidth, buildHeight, regionPositions)) {
                validCandidates.add(pos);
            }
        }
        if (validCandidates.isEmpty()) {
            return null;
        }
        validCandidates.sort((p1, p2) -> {
            int dist1 = referencePoint != null ? p1.getApproxDistance(referencePoint) : 0;
            int dist2 = referencePoint != null ? p2.getApproxDistance(referencePoint) : 0;
            int mapWidth = Games.game.mapWidth();
            int mapHeight = Games.game.mapHeight();
            int edgeDist1 = Math.min(Math.min(p1.getX(), mapWidth - 1 - p1.getX()),
                    Math.min(p1.getY(), mapHeight - 1 - p1.getY()));
            int edgeDist2 = Math.min(Math.min(p2.getX(), mapWidth - 1 - p2.getX()),
                    Math.min(p2.getY(), mapHeight - 1 - p2.getY()));
            double score1 = edgeDist1 * 0.7 - dist1 * 0.3;
            double score2 = edgeDist2 * 0.7 - dist2 * 0.3;
            return Double.compare(score1, score2);
        });
        return validCandidates.get(0);
    }

    /**
     * BFS 搜索合适的建筑位置
     *
     * @param startPos        起始搜索点
     * @param regionPositions 区域内的所有位置集合
     * @return 合适的 TilePosition
     */
    private static TilePosition bfsSearch(Task task, TilePosition startPos,
                                          Set<TilePosition> regionPositions) {
        int buildWidth = task.getBuildingType().tileWidth() + task.getXOffset();
        int buildHeight = task.getBuildingType().tileHeight() + task.getYOffset();

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
            if (canPlaceBuilding(current, task, buildWidth, buildHeight, regionPositions)) {
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

    private static boolean canPlaceBuilding(TilePosition current, Task task, int requiredWidth, int requiredHeight,
                                            Set<TilePosition> regionPositions) {
        if (!LocationValidator.isValid(current, task)) {
            return false;
        }
        for (int dx = 0; dx < requiredWidth; dx++) {
            for (int dy = 0; dy < requiredHeight; dy++) {
                TilePosition tile = new TilePosition(current.getX() + dx, current.getY() + dy);
                if (!regionPositions.contains(tile)) {
                    return false;
                }
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

    /**
     * 标记位置为失败
     */
    public static void markFailedPosition(TilePosition position) {
        if (position != null) {
            String posKey = position.getX() + "," + position.getY();
            failedPositions.add(posKey);
            System.out.println("[Positions] 标记失败位置: " + posKey);
        }
    }

    public static void resetFailedPositions() {
        failedPositions.clear();
        System.out.println("[Positions] 已重置失败位置记录");
    }
}
