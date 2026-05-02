package com.yangky.scbotdemo.util;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Positions - 建筑位置选择工具类（基于 Locations 预计算区域）
 * <p>
 * 区域划分（由 Locations 预计算）：
 * 1. MINERAL - 矿区（Base周围 + 矿附近）
 * 2. CHOKE_POINT - 路口/出口附近
 * 3. EDGE - 边缘（距离边界3格内）
 * 4. CENTRAL - 腹地（内部可建造区域）
 * 5. BOUNDARY - 边界（不可建造地形）
 *
 * @author yangky
 * @Date 2026/4/27 22:50
 */
public class Positions {

    private static final boolean DEBUG_LOG = true;
    private static final Set<String> failedPositions = ConcurrentHashMap.newKeySet();

    // ==================== 公共API ====================

    /**
     * Supply选址：边缘区域（基于 Locations 预计算的 EDGE 区域）
     */
    public static TilePosition getEdgePosition(UnitType building, Unit base, Integer edgeDistance) {
        if (base == null || !base.exists() || !Locations.isInitialized()) {
            return null;
        }
        int dist = edgeDistance != null ? edgeDistance : 3;
        TilePosition basePos = base.getTilePosition();
        log("[EdgeZone] 开始搜索 - 基地: " + basePos + ", 边缘距离: " + dist);
        // 直接从 Locations 获取 EDGE 区域候选
        Set<TilePosition> edgeTiles = Locations.getPositionsByRegion(Location.RegionType.EDGE);
        List<TilePosition> candidates = new ArrayList<>();
        for (TilePosition pos : edgeTiles) {
            if (isCandidateValid(pos, building, basePos, dist)) {
                candidates.add(pos);
            }
        }
        log("[EdgeZone] 生成候选: " + candidates.size() + " 个");
        TilePosition result = selectWeightedRandomEdge(candidates, dist);
        if (LocationValidator.isValid(result, building) && isReachable(result)) {
            log("[EdgeZone] ✓ 选择位置: " + result);
            return result;
        }
        log("[EdgeZone] 验证失败，尝试备选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building) && isReachable(candidate)) {
                log("[EdgeZone] ✓ 选择备选位置: " + candidate);
                return candidate;
            }
        }
        log("[EdgeZone] 验证失败，使用中心区域");
        Set<TilePosition> centralTiles = Locations.getPositionsByRegion(Location.RegionType.CENTRAL);
        for (TilePosition pos : centralTiles) {
            if (LocationValidator.isValid(pos, building) && isReachable(pos)) {
                log("[EdgeZone] ✓ 选择中心位置: " + pos);
                return pos;
            }
        }
        log("[EdgeZone] ✗ 所有候选均无效，使用降级方案");
        return Games.game.getBuildLocation(building, basePos, 20);
    }

    /**
     * Supply选址（使用默认参数）
     */
    public static TilePosition getEdgePosition(UnitType building, Unit base) {
        return getEdgePosition(building, base, 3);
    }

    /**
     * 内圈选址：Barracks等早期战斗建筑（基于 Locations 的 CENTRAL 区域，靠近基地）
     */
    public static TilePosition getInnerRingPosition(UnitType building, Unit base) {
        if (base == null || !base.exists() || !Locations.isInitialized()) {
            return null;
        }
        TilePosition basePos = base.getTilePosition();
        log("[InnerRing] 开始搜索 - 基地: " + basePos);
        // 从 CENTRAL 区域筛选距离基地 5-10 格的候选
        Set<TilePosition> centralTiles = Locations.getPositionsByRegion(Location.RegionType.CENTRAL);
        List<TilePosition> candidates = new ArrayList<>();
        for (TilePosition pos : centralTiles) {
            int distToBase = basePos.getApproxDistance(pos);
            if (distToBase >= 5 && distToBase <= 10 && isCandidateValid(pos, building, basePos, null)) {
                candidates.add(pos);
            }
        }
        log("[InnerRing] 生成候选: " + candidates.size() + " 个");
        TilePosition result = selectWeightedRandomInner(candidates, basePos);
        if (LocationValidator.isValid(result, building) && isReachable(result)) {
            log("[InnerRing] ✓ 选择位置: " + result);
            return result;
        }
        log("[InnerRing] 验证失败，尝试备选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building) && isReachable(candidate)) {
                log("[InnerRing] ✓ 选择备选位置: " + candidate);
                return candidate;
            }
        }
        log("[InnerRing] 所有候选均无效，使用降级方案");
        return Games.game.getBuildLocation(building, basePos, 15);
    }

    /**
     * 腹地选址：Factory等中期战斗建筑（基于 Locations 的 CENTRAL 区域，远离基地）
     */
    public static TilePosition getCentralPosition(UnitType building, Unit base) {
        if (base == null || !base.exists() || !Locations.isInitialized()) {
            return null;
        }
        TilePosition basePos = base.getTilePosition();
        log("[CENTRAL] 开始搜索 - 基地: " + basePos);
        // 从 CENTRAL 区域筛选距离基地
        Set<TilePosition> centralTiles = Locations.getPositionsByRegion(Location.RegionType.CENTRAL);
        List<TilePosition> candidates = new ArrayList<>();
        for (TilePosition pos : centralTiles) {
//            int distToBase = basePos.getApproxDistance(pos);
            if (/*distToBase > 10 && */isCandidateValid(pos, building, basePos, null)) {
                candidates.add(pos);
            }
        }
        log("[CENTRAL] 生成候选: " + candidates.size() + " 个");
        TilePosition result = selectWeightedRandomInner(candidates, basePos);
        if (LocationValidator.isValid(result, building) && isReachable(result)) {
            log("[CENTRAL] ✓ 选择位置: " + result);
            return result;
        }
        log("[CENTRAL] 验证失败，尝试备选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building) && isReachable(candidate)) {
                log("[CENTRAL] ✓ 选择备选位置: " + candidate);
                return candidate;
            }
        }
        // 放宽条件：仅验证位置合法性，不检查可达性
        log("[Heartland] 放宽条件，仅验证位置合法性");
        for (TilePosition candidate : candidates) {
            if (LocationValidator.isValid(candidate, building)) {
                log("[Heartland] ✓ 选择位置（忽略可达性）: " + candidate);
                return candidate;
            }
        }
        log("[Heartland] ✗ 所有候选均无效，使用降级方案");
        TilePosition fallback = Games.game.getBuildLocation(building, basePos, 25);
        if (fallback != null) {
            log("[Heartland] 降级方案位置: " + fallback);
            return fallback;
        }
        log("[Heartland] ✗ 降级方案也失败，返回 null");
        return null;
    }

    /**
     * 路口选址：堵口建筑（Bunker/Wall）（基于 Locations 的 CHOKE_POINT 区域）
     */
    public static TilePosition getChokePointPosition(UnitType building, TilePosition chokePoint) {
        if (chokePoint == null || !Locations.isInitialized()) {
            return null;
        }
        log("[ChokePoint] 使用路口位置: " + chokePoint);
        // 优先直接使用传入的 chokePoint
        if (LocationValidator.isValid(chokePoint, building)) {
            return chokePoint;
        }
        // 从 CHOKE_POINT 区域筛选候选
        Set<TilePosition> chokeTiles = Locations.getPositionsByRegion(Location.RegionType.CHOKE_POINT);
        for (TilePosition candidate : chokeTiles) {
            if (LocationValidator.isValid(candidate, building)) {
                log("[ChokePoint] ✓ 选择位置: " + candidate);
                return candidate;
            }
        }
        log("[ChokePoint] ✗ 未找到合适位置");
        return null;
    }
    // ==================== 候选验证 ====================

    /**
     * 验证候选位置是否满足基本条件
     */
    private static boolean isCandidateValid(TilePosition pos, UnitType building, TilePosition basePos, Integer edgeDistance) {
        // 排除已失败的位置
        String posKey = pos.getX() + "," + pos.getY();
        if (failedPositions.contains(posKey)) {
            return false;
        }
        // 动态检查可建造性（新建筑会改变地图状态）
        if (!Games.isBuildable(pos)) {
            return false;
        }
        // 检查建筑尺寸是否超出地图边界
        int buildWidth = building != null ? building.tileWidth() : 1;
        int buildHeight = building != null ? building.tileHeight() : 1;
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        if (pos.getX() + buildWidth > mapWidth || pos.getY() + buildHeight > mapHeight) {
            return false;
        }
        // 边缘区域：检查距离边缘是否符合要求
        if (edgeDistance != null) {
            int distToEdge = getDistanceToEdge(pos.getX(), pos.getY(), mapWidth, mapHeight);
            return distToEdge <= edgeDistance;
        }
        return true;
    }

    /**
     * 检查位置是否可达（简化版：检查周围是否有通路）
     */
    private static boolean isReachable(TilePosition target) {
        if (target == null) {
            return false;
        }
        // 检查目标位置周围 3x3 是否有可通行的格子
        int accessibleNeighbors = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                TilePosition neighbor = new TilePosition(target.getX() + dx, target.getY() + dy);
                if (isInMap(neighbor) && Games.isBuildable(neighbor)) {
                    accessibleNeighbors++;
                }
            }
        }
        // 周围至少有 2 个可通行格子才算可达
        return accessibleNeighbors >= 2;
    }

    /**
     * 边缘区域加权随机（优先靠外，但避免贴边）
     */
    private static TilePosition selectWeightedRandomEdge(List<TilePosition> candidates, int edgeDistance) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        Map<TilePosition, Double> weights = new HashMap<>();
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        for (TilePosition pos : candidates) {
            int distToEdge = getDistanceToEdge(pos.getX(), pos.getY(), mapWidth, mapHeight);
            // 基础权重：距离边缘越近越好（但要在 edgeDistance 范围内）
            double weight = 1.0 / (Math.abs(distToEdge - edgeDistance) + 1);
            // 惩罚因子：紧贴边缘（distToEdge == 0）大幅降低权重
            if (distToEdge == 0) {
                weight *= 0.3;
            } else if (distToEdge == 1) {
                weight *= 0.6;
            }
            weights.put(pos, weight);
        }
        return weightedRandomSelect(weights);
    }

    /**
     * 内圈/腹地加权随机（优先靠近基地）
     */
    private static TilePosition selectWeightedRandomInner(List<TilePosition> candidates, TilePosition basePos) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        Map<TilePosition, Double> weights = new HashMap<>();
        for (TilePosition pos : candidates) {
            int distToBase = basePos.getApproxDistance(pos);
            double weight = 1.0 / (distToBase + 1);
            weights.put(pos, weight);
        }

        return weightedRandomSelect(weights);
    }

    private static TilePosition weightedRandomSelect(Map<TilePosition, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) {
            List<TilePosition> positions = new ArrayList<>(weights.keySet());
            return positions.get((int) (Math.random() * positions.size()));
        }

        double random = Math.random() * totalWeight;
        double cumulative = 0;
        List<Map.Entry<TilePosition, Double>> entries = new ArrayList<>(weights.entrySet());
        java.util.Collections.shuffle(entries);

        for (Map.Entry<TilePosition, Double> entry : entries) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    /**
     * 计算位置到地图边缘的最短距离
     */
    private static int getDistanceToEdge(int x, int y, int mapWidth, int mapHeight) {
        return Math.min(Math.min(x, mapWidth - 1 - x), Math.min(y, mapHeight - 1 - y));
    }

    /**
     * 检查位置是否在地图内
     */
    private static boolean isInMap(TilePosition pos) {
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        return pos.getX() >= 0 && pos.getY() >= 0 && pos.getX() < mapWidth && pos.getY() < mapHeight;
    }

    /**
     * 日志输出控制
     */
    private static void log(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }

    /**
     * 清除失败位置记录
     */
    public static void resetFailedPositions() {
        failedPositions.clear();
        log("[Positions] 已清除失败位置记录");
    }

    /**
     * 标记位置为失败
     */
    public static void markFailedPosition(TilePosition position) {
        if (position != null) {
            String posKey = position.getX() + "," + position.getY();
            failedPositions.add(posKey);
            log("[Positions] 标记失败位置: " + posKey);
        }
    }

    /**
     * 防空选址：边缘区域，间隔6-8格
     */
    public static TilePosition getMissilePosition(Unit base) {
        if (base == null || !base.exists() || !Locations.isInitialized()) {
            return null;
        }
        TilePosition basePos = base.getTilePosition();
        Set<TilePosition> edgeTiles = Locations.getPositionsByRegion(Location.RegionType.EDGE);
        List<TilePosition> edgeSorted = edgeTiles.stream()
                .sorted(Comparator.comparingInt(e -> -e.getApproxDistance(basePos))).collect(Collectors.toList());
        Set<Unit> existingAAs = Units.getSelfUnits(UnitType.Terran_Missile_Turret);
        int minSpacing = 5; // 间隔5格
        for (TilePosition pos : edgeSorted) {
            // 基础验证
            if (!isCandidateValid(pos, UnitType.Terran_Missile_Turret, basePos, null)) {
                continue;
            }
            // 检查与所有已有防空的距离
            boolean isFarEnough = true;
            for (Unit aa : existingAAs) {
                if (aa.getTilePosition().getApproxDistance(pos) < minSpacing) {
                    isFarEnough = false;
                    break;
                }
            }
            if (isFarEnough) {
                log("[MissileTurrets] ✓ 选择位置: " + pos);
                return pos;
            }
        }
        Set<TilePosition> mineralPositions = Locations.getPositionsByRegion(Location.RegionType.MINERAL);
        for (TilePosition pos : mineralPositions) {
            if (!isCandidateValid(pos, UnitType.Terran_Missile_Turret, basePos, null)) {
                continue;
            }
            // 检查与所有已有防空的距离
            boolean isFarEnough = true;
            for (Unit aa : existingAAs) {
                if (aa.getTilePosition().getApproxDistance(pos) < minSpacing) {
                    isFarEnough = false;
                    break;
                }
            }
            // 检查与最近的水晶、气矿的距离
            if (isFarEnough && LocationValidator.isValid(pos, UnitType.Terran_Missile_Turret)) {
                log("[MissileTurrets] ✓ 选择位置: " + pos);
                return pos;
            }
        }
        log("[MissileTurrets] ✗ 未找到合适位置（可能已布满）");
        return null;
    }

}
