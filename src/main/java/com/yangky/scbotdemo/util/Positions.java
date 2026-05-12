//package com.yangky.scbotdemo.util;
//
//import bwapi.TilePosition;
//import bwapi.Unit;
//import bwapi.UnitType;
//import com.yangky.scbotdemo.bwem.Games;
//import com.yangky.scbotdemo.bwem.LocationValidator;
//import com.yangky.scbotdemo.bwem.Locations;
//import com.yangky.scbotdemo.bwem.Units;
//import com.yangky.scbotdemo.bwem.region.RegionType;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
///**
// * Positions - 建筑位置选择工具类（基于 Locations 预计算区域）
// * <p>
// * 区域划分（由 Locations 预计算）：
// * 1. MINERAL - 矿区（Base周围 + 矿附近）
// * 2. CHOKE_POINT - 路口/出口附近
// * 3. EDGE - 边缘（距离边界3格内）
// * 4. CENTRAL - 腹地（内部可建造区域）
// * 5. BOUNDARY - 边界（不可建造地形）
// *
// * @author yangky
// * @Date 2026/4/27 22:50
// */
//public class Positions {
//
//    private static final boolean DEBUG_LOG = true;
//    private static final Set<String> failedPositions = ConcurrentHashMap.newKeySet();
//
//    // ==================== 公共API ====================
//
//    /**
//     * 验证候选位置是否满足基本条件
//     */
//    private static boolean isCandidateValid(TilePosition pos, UnitType building, TilePosition basePos, Integer edgeDistance) {
//        // 排除已失败的位置
//        String posKey = pos.getX() + "," + pos.getY();
//        if (failedPositions.contains(posKey)) {
//            return false;
//        }
//        // 动态检查可建造性（新建筑会改变地图状态）
//        if (!Games.isBuildable(pos)) {
//            return false;
//        }
//        // 检查建筑尺寸是否超出地图边界
//        int buildWidth = building != null ? building.tileWidth() : 1;
//        int buildHeight = building != null ? building.tileHeight() : 1;
//        int mapWidth = Games.game.mapWidth();
//        int mapHeight = Games.game.mapHeight();
//        if (pos.getX() + buildWidth > mapWidth || pos.getY() + buildHeight > mapHeight) {
//            return false;
//        }
//        // 边缘区域：检查距离边缘是否符合要求
//        if (edgeDistance != null) {
//            int distToEdge = getDistanceToEdge(pos.getX(), pos.getY(), mapWidth, mapHeight);
//            return distToEdge <= edgeDistance;
//        }
//        return true;
//    }
//
//    /**
//     * 检查位置是否可达（简化版：检查周围是否有通路）
//     */
//    private static boolean isReachable(TilePosition target) {
//        if (target == null) {
//            return false;
//        }
//        // 检查目标位置周围 3x3 是否有可通行的格子
//        int accessibleNeighbors = 0;
//        for (int dx = -1; dx <= 1; dx++) {
//            for (int dy = -1; dy <= 1; dy++) {
//                if (dx == 0 && dy == 0) continue;
//                TilePosition neighbor = new TilePosition(target.getX() + dx, target.getY() + dy);
//                if (isInMap(neighbor) && Games.isBuildable(neighbor)) {
//                    accessibleNeighbors++;
//                }
//            }
//        }
//        // 周围至少有 2 个可通行格子才算可达
//        return accessibleNeighbors >= 2;
//    }
//
//    /**
//     * 边缘区域加权随机（优先靠外，但避免贴边）
//     */
//    private static TilePosition selectWeightedRandomEdge(List<TilePosition> candidates, int edgeDistance) {
//        if (candidates.isEmpty()) return null;
//        if (candidates.size() == 1) return candidates.get(0);
//
//        Map<TilePosition, Double> weights = new HashMap<>();
//        int mapWidth = Games.game.mapWidth();
//        int mapHeight = Games.game.mapHeight();
//
//        for (TilePosition pos : candidates) {
//            int distToEdge = getDistanceToEdge(pos.getX(), pos.getY(), mapWidth, mapHeight);
//            // 基础权重：距离边缘越近越好（但要在 edgeDistance 范围内）
//            double weight = 1.0 / (Math.abs(distToEdge - edgeDistance) + 1);
//            // 惩罚因子：紧贴边缘（distToEdge == 0）大幅降低权重
//            if (distToEdge == 0) {
//                weight *= 0.3;
//            } else if (distToEdge == 1) {
//                weight *= 0.6;
//            }
//            weights.put(pos, weight);
//        }
//        return weightedRandomSelect(weights);
//    }
//
//    /**
//     * 内圈/腹地加权随机（优先靠近基地）
//     */
//    private static TilePosition selectWeightedRandomInner(List<TilePosition> candidates, TilePosition basePos) {
//        if (candidates.isEmpty()) return null;
//        if (candidates.size() == 1) return candidates.get(0);
//
//        Map<TilePosition, Double> weights = new HashMap<>();
//        for (TilePosition pos : candidates) {
//            int distToBase = basePos.getApproxDistance(pos);
//            double weight = 1.0 / (distToBase + 1);
//            weights.put(pos, weight);
//        }
//
//        return weightedRandomSelect(weights);
//    }
//
//    private static TilePosition weightedRandomSelect(Map<TilePosition, Double> weights) {
//        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
//        if (totalWeight == 0) {
//            List<TilePosition> positions = new ArrayList<>(weights.keySet());
//            return positions.get((int) (Math.random() * positions.size()));
//        }
//
//        double random = Math.random() * totalWeight;
//        double cumulative = 0;
//        List<Map.Entry<TilePosition, Double>> entries = new ArrayList<>(weights.entrySet());
//        java.util.Collections.shuffle(entries);
//
//        for (Map.Entry<TilePosition, Double> entry : entries) {
//            cumulative += entry.getValue();
//            if (random <= cumulative) {
//                return entry.getKey();
//            }
//        }
//        return weights.keySet().iterator().next();
//    }
//
//    /**
//     * 计算位置到地图边缘的最短距离
//     */
//    private static int getDistanceToEdge(int x, int y, int mapWidth, int mapHeight) {
//        return Math.min(Math.min(x, mapWidth - 1 - x), Math.min(y, mapHeight - 1 - y));
//    }
//
//    /**
//     * 检查位置是否在地图内
//     */
//    private static boolean isInMap(TilePosition pos) {
//        int mapWidth = Games.game.mapWidth();
//        int mapHeight = Games.game.mapHeight();
//        return pos.getX() >= 0 && pos.getY() >= 0 && pos.getX() < mapWidth && pos.getY() < mapHeight;
//    }
//
//    /**
//     * 日志输出控制
//     */
//    private static void log(String message) {
//        if (DEBUG_LOG) {
//            System.out.println(message);
//        }
//    }
//
//    /**
//     * 清除失败位置记录
//     */
//    public static void resetFailedPositions() {
//        failedPositions.clear();
//        log("[Positions] 已清除失败位置记录");
//    }
//
//    /**
//     * 标记位置为失败
//     */
//    public static void markFailedPosition(TilePosition position) {
//        if (position != null) {
//            String posKey = position.getX() + "," + position.getY();
//            failedPositions.add(posKey);
//            log("[Positions] 标记失败位置: " + posKey);
//        }
//    }
//
//    public static TilePosition getMissilePosition(Unit base) {
//        if (base == null || !base.exists() || !Locations.isInitialized()) {
//            return null;
//        }
//        TilePosition basePos = base.getTilePosition();
//        Set<TilePosition> edgeTiles = Locations.getPositionsByRegion(RegionType.EDGE);
//        List<TilePosition> edgeSorted = edgeTiles.stream()
//                .sorted(Comparator.comparingInt(e -> -e.getApproxDistance(basePos))).collect(Collectors.toList());
//        Set<Unit> existingAAs = Units.getSelfUnits(UnitType.Terran_Missile_Turret);
//        int minSpacing = 5;
//
//        // ✅ 第一优先：EDGE 区域 + 间距检查
//        for (TilePosition pos : edgeSorted) {
//            if (!isCandidateValid(pos, UnitType.Terran_Missile_Turret, basePos, null)) {
//                continue;
//            }
//            boolean isFarEnough = true;
//            for (Unit aa : existingAAs) {
//                if (aa.getTilePosition().getApproxDistance(pos) < minSpacing) {
//                    isFarEnough = false;
//                    break;
//                }
//            }
//            if (isFarEnough) {
//                log("[MissileTurrets] ✓ 选择 EDGE 位置: " + pos);
//                return pos;
//            }
//        }
//
//        // ✅ 第二优先：MINERAL 区域 + 间距检查
//        Set<TilePosition> mineralPositions = Locations.getPositionsByRegion(RegionType.MINERAL);
//        for (TilePosition pos : mineralPositions) {
//            if (!isCandidateValid(pos, UnitType.Terran_Missile_Turret, basePos, null)) {
//                continue;
//            }
//            boolean isFarEnough = true;
//            for (Unit aa : existingAAs) {
//                if (aa.getTilePosition().getApproxDistance(pos) < minSpacing) {
//                    isFarEnough = false;
//                    break;
//                }
//            }
//            if (isFarEnough && LocationValidator.isValid(pos, UnitType.Terran_Missile_Turret)) {
//                log("[MissileTurrets] ✓ 选择 MINERAL 位置: " + pos);
//                return pos;
//            }
//        }
//
//        // ✅ 第三优先：使用 BWAPI 默认方法（兜底）
//        log("[MissileTurrets] 自定义选址失败，使用 BWAPI 默认方法");
//        TilePosition fallback = Games.game.getBuildLocation(UnitType.Terran_Missile_Turret, basePos, 30);
//        if (fallback != null) {
//            log("[MissileTurrets] ✓ 降级方案位置: " + fallback);
//            return fallback;
//        }
//
//        log("[MissileTurrets] ✗ 未找到合适位置");
//        return null;
//    }
//
//}
