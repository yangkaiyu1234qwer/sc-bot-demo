package com.yangky.scbotdemo.util;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.ChokePoint;
import bwem.Neutral;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.LocationValidator;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.yangky.scbotdemo.bwem.Actions.DETOUR_DISTANCE;

/**
 * Positions - 建筑位置选择工具类（四区域模型）
 * <p>
 * 区域划分：
 * 1. 路口区域 (ChokePoint Zone) - ChokePoint ± 3格，用于堵口建筑
 * 2. 矿区区域 (Mineral Zone) - Base周围0-5格 + 矿±2格，用于Base/Refinery
 * 3. 内圈区域 (Inner Ring) - 5-10格，用于Barracks等早期战斗建筑
 * 4. 腹地区域 (Heartland Zone) - 10格到边缘内边界，用于Factory等中期建筑
 * 5. 边缘区域 (Edge Zone) - 距离边缘edgeDist-3到edgeDist格，用于Supply/防空
 *
 * @author yangky
 * @Date 2026/4/27 22:50
 */
public class Positions {

    private static final boolean DEBUG_LOG = true;

    private static final Set<String> failedPositions = ConcurrentHashMap.newKeySet();

    // ==================== 公共API ====================

    /**
     * Supply选址：边缘区域
     *
     * @param building     建筑类型
     * @param base         主基地
     * @param edgeDistance 距离地图边缘的距离（默认3，0表示贴边）
     * @return 合适的建造位置
     */
    public static TilePosition getEdgePosition(UnitType building, Unit base, Integer edgeDistance) {
        if (base == null || !base.exists()) {
            return null;
        }
        int dist = edgeDistance != null ? edgeDistance : 3;
        TilePosition basePos = base.getTilePosition();
        TilePosition chokePoint = getNearestChokePoint(basePos);
        log("[EdgeZone] 开始搜索 - 基地: " + basePos + ", 边缘距离: " + dist);
        List<TilePosition> candidates = generateEdgeCandidates(basePos, dist, chokePoint);
        log("[EdgeZone] 生成候选: " + candidates.size() + " 个");
        // ✅ 打印前5个候选的位置和距离信息
        if (!candidates.isEmpty() && DEBUG_LOG) {
            int mapWidth = Games.game.mapWidth();
            int mapHeight = Games.game.mapHeight();
            log("[EdgeZone] === 候选位置详情（前5个）===");
            for (int i = 0; i < Math.min(5, candidates.size()); i++) {
                TilePosition candidate = candidates.get(i);
                int distToEdge = getDistanceToEdge(candidate.getX(), candidate.getY(), mapWidth, mapHeight);
                int distFromBase = basePos.getApproxDistance(candidate);
                log("[EdgeZone]   候选" + (i + 1) + ": " + candidate
                        + ", 距边缘=" + distToEdge
                        + ", 距基地=" + distFromBase);
            }
        }
        TilePosition result = selectWeightedRandom(candidates, true);
        // ✅ 打印选中位置的信息
        if (result != null && DEBUG_LOG) {
            int mapWidth = Games.game.mapWidth();
            int mapHeight = Games.game.mapHeight();
            int distToEdge = getDistanceToEdge(result.getX(), result.getY(), mapWidth, mapHeight);
            int distFromBase = basePos.getApproxDistance(result);
            log("[EdgeZone] 加权随机选中: " + result
                    + ", 距边缘=" + distToEdge
                    + ", 距基地=" + distFromBase);
        }
        if (LocationValidator.isValid(result, building)) {
            if (isReachableFromBase(base, result)) {
                log("[EdgeZone] ✓ 验证通过，选择位置: " + result);
                return result;
            } else {
                log("[EdgeZone] 位置不可达，尝试其他候选: " + result);
            }
        }
        log("[EdgeZone] 验证失败，尝试其他候选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building)) {
                if (isReachableFromBase(base, candidate)) {
                    int distToEdge = getDistanceToEdge(candidate.getX(), candidate.getY(),
                            Games.game.mapWidth(), Games.game.mapHeight());
                    int distFromBase = basePos.getApproxDistance(candidate);
                    log("[EdgeZone] ✓ 选择备选位置: " + candidate
                            + ", 距边缘=" + distToEdge
                            + ", 距基地=" + distFromBase);
                    return candidate;
                } else {
                    log("[EdgeZone] 备选位置不可达: " + candidate);
                }
            }
        }
        // 尝试扩大搜索范围重新生成候选
        if (dist < 10) {
            log("[EdgeZone] 扩大搜索范围重试: " + (dist + 2));
            return getEdgePosition(building, base, dist + 2);
        }
        log("[EdgeZone] ✗ 所有候选均无效，使用降级方案");
        TilePosition fallback = Games.game.getBuildLocation(building, basePos, 20);
        if (fallback != null) {
            log("[EdgeZone] 降级方案位置: " + fallback);
            return fallback;
        }
        log("[EdgeZone] ✗ 降级方案也失败，返回 null");
        return fallback;
    }

    private static boolean isNearWallOff(TilePosition pos) {
        // 检查是否在堵口建筑附近（5 格范围内）
        Set<String> wallOffPositions = WallOffExecutor.getWallOffPositions();
        for (String wallOffPos : wallOffPositions) {
            String[] parts = wallOffPos.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            if (Math.abs(pos.getX() - x) <= 5 && Math.abs(pos.getY() - y) <= 5) {
                return true;
            }
        }
        return false;
    }


    // ... existing code ...

    private static boolean isReachableFromBase(Unit base, TilePosition target) {
        if (base == null || target == null) {
            return false;
        }
        int distance = base.getTilePosition().getApproxDistance(target);
        if (distance > 30) {
            return false;
        }
        // ✅ 检查是否紧贴地图边缘
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        boolean isOnEdge = target.getX() <= 2 || target.getX() >= mapWidth - 3 ||
                target.getY() <= 2 || target.getY() >= mapHeight - 3;
        if (isOnEdge) {
            // 如果在边缘，检查是否有足够的空间让 SCV 接近
            int accessibleNeighbors = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    TilePosition neighbor = new TilePosition(target.getX() + dx, target.getY() + dy);
                    if (isInMap(neighbor) && Games.isBuildable(neighbor)) {
                        accessibleNeighbors++;
                    }
                }
            }
            // 如果周围可通行的格子少于 5 个，说明是死角
            if (accessibleNeighbors < 5) {
                return false;
            }
        }
        // 检查目标位置周围是否有通路
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                TilePosition neighbor = new TilePosition(target.getX() + dx, target.getY() + dy);
                if (isInMap(neighbor) && Games.isBuildable(neighbor)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Supply选址（使用默认参数）
     */
    public static TilePosition getEdgePosition(UnitType building, Unit base) {
        return getEdgePosition(building, base, 3);
    }

    /**
     * 内圈选址：Barracks等早期战斗建筑
     *
     * @param building 建筑类型
     * @param base     主基地
     * @return 合适的建造位置
     */
    public static TilePosition getInnerRingPosition(UnitType building, Unit base) {
        if (base == null || !base.exists()) {
            return null;
        }

        TilePosition basePos = base.getTilePosition();
        TilePosition chokePoint = getNearestChokePoint(basePos);

        log("[InnerRing] 开始搜索 - 基地: " + basePos);

        List<TilePosition> candidates = generateRingCandidates(basePos, 5, 10, chokePoint, building);
        log("[InnerRing] 生成候选: " + candidates.size() + " 个");

        TilePosition result = selectWeightedRandom(candidates, false, basePos);
        if (LocationValidator.isValid(result, building)) {
            log("[InnerRing] 选择位置: " + result);
            return result;
        }

        log("[InnerRing] 验证失败，尝试其他候选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building)) {
                log("[InnerRing] 选择备选位置: " + candidate);
                return candidate;
            }
        }

        log("[InnerRing] 所有候选均无效，使用降级方案");
        return Games.game.getBuildLocation(building, basePos, 15);
    }

    /**
     * 腹地选址：Factory等中期战斗建筑
     *
     * @param building 建筑类型
     * @param base     主基地
     * @return 合适的建造位置
     */
    public static TilePosition getHeartlandPosition(UnitType building, Unit base) {
        if (base == null || !base.exists()) {
            return null;
        }

        TilePosition basePos = base.getTilePosition();
        TilePosition chokePoint = getNearestChokePoint(basePos);
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        int distToEdge = getDistanceToEdge(basePos.getX(), basePos.getY(), mapWidth, mapHeight);
        int outerRadius = Math.max(15, distToEdge - 4);

        log("[Heartland] 开始搜索 - 基地: " + basePos + ", 外半径: " + outerRadius);

        List<TilePosition> candidates = generateRingCandidates(basePos, 10, outerRadius, chokePoint, building);
        log("[Heartland] 生成候选: " + candidates.size() + " 个");

        TilePosition result = selectWeightedRandom(candidates, false, basePos);
        if (LocationValidator.isValid(result, building)) {
            log("[Heartland] 选择位置: " + result);
            return result;
        }

        log("[Heartland] 验证失败，尝试其他候选");
        for (TilePosition candidate : candidates) {
            if (!candidate.equals(result) && LocationValidator.isValid(candidate, building)) {
                log("[Heartland] 选择备选位置: " + candidate);
                return candidate;
            }
        }

        log("[Heartland] 所有候选均无效，使用降级方案");
        return Games.game.getBuildLocation(building, basePos, 20);
    }

    /**
     * 路口选址：堵口建筑（Bunker/Wall）
     *
     * @param building   建筑类型
     * @param chokePoint 路口位置
     * @return 合适的建造位置
     */
    public static TilePosition getChokePointPosition(UnitType building, TilePosition chokePoint) {
        if (chokePoint == null) {
            return null;
        }

        log("[ChokePoint] 使用路口位置: " + chokePoint);

        if (LocationValidator.isValid(chokePoint, building)) {
            return chokePoint;
        }

        log("[ChokePoint] 路口位置无效，搜索附近位置");
        List<TilePosition> candidates = generateRingCandidates(chokePoint, 0, 3, null, building);
        for (TilePosition candidate : candidates) {
            if (LocationValidator.isValid(candidate, building)) {
                log("[ChokePoint] 选择位置: " + candidate);
                return candidate;
            }
        }

        log("[ChokePoint] 未找到合适位置");
        return null;
    }

    // ==================== 候选生成器 ====================

    /**
     * 计算绕行位置（避开障碍物，朝目标方向绕行）
     */
    private static Position calculateDetourPosition(Position currentPos, Position obstaclePos, Position target) {
        double toTargetX = target.getX() - currentPos.getX();
        double toTargetY = target.getY() - currentPos.getY();

        double dx = currentPos.getX() - obstaclePos.getX();
        double dy = currentPos.getY() - obstaclePos.getY();

        int mapWidth = Games.game.mapWidth() * 32;  // ✅ 转换为像素坐标
        int mapHeight = Games.game.mapHeight() * 32;

        Position detourPos;
        if (Math.abs(dx) > Math.abs(dy)) {
            int detourY = toTargetY >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
            detourPos = new Position(obstaclePos.getX(), obstaclePos.getY() + detourY);
        } else {
            int detourX = toTargetX >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
            detourPos = new Position(obstaclePos.getX() + detourX, obstaclePos.getY());
        }

        // ✅ 确保绕行点在地图范围内
        int clampedX = Math.max(0, Math.min(detourPos.getX(), mapWidth));
        int clampedY = Math.max(0, Math.min(detourPos.getY(), mapHeight));

        return new Position(clampedX, clampedY);
    }


    /**
     * 判断位置是否在"矿后"（矿和边缘之间）
     */
    private static boolean isPositionBehindMinerals(TilePosition pos, List<bwapi.Unit> minerals, TilePosition basePos) {
        if (minerals == null || minerals.isEmpty()) {
            return false;
        }

        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        // 计算位置到最近边缘的方向
        int distToLeft = pos.getX();
        int distToRight = mapWidth - 1 - pos.getX();
        int distToTop = pos.getY();
        int distToBottom = mapHeight - 1 - pos.getY();

        int minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToTop, distToBottom));

        // 判断主要方向
        boolean isLeftEdge = (distToLeft == minDist);
        boolean isRightEdge = (distToRight == minDist);
        boolean isTopEdge = (distToTop == minDist);
        boolean isBottomEdge = (distToBottom == minDist);

        // 检查是否有矿在这个位置和基地之间
        for (bwapi.Unit mineral : minerals) {
            TilePosition mineralPos = mineral.getTilePosition();

            // 矿应该在基地和边缘位置之间
            if (isLeftEdge && mineralPos.getX() < pos.getX() && mineralPos.getX() > basePos.getX()) {
                return true;
            }
            if (isRightEdge && mineralPos.getX() > pos.getX() && mineralPos.getX() < basePos.getX()) {
                return true;
            }
            if (isTopEdge && mineralPos.getY() < pos.getY() && mineralPos.getY() > basePos.getY()) {
                return true;
            }
            if (isBottomEdge && mineralPos.getY() > pos.getY() && mineralPos.getY() < basePos.getY()) {
                return true;
            }
        }

        return false;
    }

    private static List<TilePosition> generateEdgeCandidates(TilePosition basePos, int edgeDistance, TilePosition chokePoint) {
        List<TilePosition> candidates = new ArrayList<>();
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        int innerBoundary = Math.max(0, edgeDistance - 3);
        int maxDistFromBase = 25;

        List<bwapi.Unit> minerals = new ArrayList<>();
        if (Bases.getMainBase() != null) {
            try {
                minerals = Bases.getMainBase().getMinerals().stream()
                        .map(Neutral::getUnit)
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                System.err.println("获取矿区信息失败: " + e.getMessage());
            }
        }

        for (int x = 0; x < mapWidth; x += 3) {
            for (int y = 0; y < mapHeight; y += 3) {
                int distToEdge = getDistanceToEdge(x, y, mapWidth, mapHeight);
                if (distToEdge >= innerBoundary && distToEdge <= edgeDistance) {
                    int distFromBase = Math.abs(x - basePos.getX()) + Math.abs(y - basePos.getY());
                    if (distFromBase > maxDistFromBase) {
                        continue;
                    }
                    TilePosition pos = new TilePosition(x, y);
                    // ✅ 排除已失败的位置
                    String posKey = pos.getX() + "," + pos.getY();
                    if (failedPositions.contains(posKey)) {
                        continue;
                    }
                    if (!Games.isBuildable(pos)) {
                        continue;
                    }
                    if (isInChokePointDirection(pos, basePos, chokePoint, 45)) {
                        continue;
                    }
                    boolean isBehindMinerals = isPositionBehindMinerals(pos, minerals, basePos);
                    if (isBehindMinerals) {
                        candidates.add(0, pos);
                    } else {
                        candidates.add(pos);
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * 生成环形区域候选
     */
    private static List<TilePosition> generateRingCandidates(TilePosition center, int innerRadius, int outerRadius, TilePosition excludeDirection, UnitType buildingType) {
        List<TilePosition> candidates = new ArrayList<>();
        int buildWidth = buildingType != null ? buildingType.tileWidth() : 1;
        int buildHeight = buildingType != null ? buildingType.tileHeight() : 1;
        for (int dx = -outerRadius; dx <= outerRadius; dx++) {
            for (int dy = -outerRadius; dy <= outerRadius; dy++) {
                int chebyshevDist = Math.max(Math.abs(dx), Math.abs(dy));
                if (chebyshevDist < innerRadius || chebyshevDist > outerRadius) {
                    continue;
                }
                TilePosition pos = new TilePosition(center.getX() + dx, center.getY() + dy);
                if (!isInMapWithBuilding(pos, buildWidth, buildHeight)) {
                    continue;
                }
                if (isInChokePointDirection(pos, center, excludeDirection, 45)) {
                    continue;
                }
                candidates.add(pos);
            }
        }
        return candidates;
    }

    private static boolean isInMapWithBuilding(TilePosition pos, int buildWidth, int buildHeight) {
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        return pos.getX() >= 0 && pos.getY() >= 0 &&
                pos.getX() + buildWidth <= mapWidth &&
                pos.getY() + buildHeight <= mapHeight;
    }

    /**
     * 加权随机选择（边缘区域：优先靠外）
     */
    private static TilePosition selectWeightedRandom(List<TilePosition> candidates, boolean preferOuter) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        Map<TilePosition, Double> weights = new HashMap<>();
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        for (TilePosition pos : candidates) {
            int distToEdge = getDistanceToEdge(pos.getX(), pos.getY(), mapWidth, mapHeight);
            // ✅ 基础权重：距离边缘越远越好
            double weight = preferOuter ? 1.0 / (distToEdge + 1) : 1.0;
            // ✅ 惩罚因子：紧贴边缘的位置权重大幅降低
            if (distToEdge == 0) {
                weight *= 0.3;  // 大幅降低权重（70% 惩罚）
            } else if (distToEdge == 1) {
                weight *= 0.6;  // 适度降低权重（40% 惩罚）
            }
            weights.put(pos, weight);
        }
        return weightedRandomSelect(weights);
    }

    /**
     * 加权随机选择（内圈/腹地：优先靠近Base）
     */
    private static TilePosition selectWeightedRandom(List<TilePosition> candidates, boolean preferOuter, TilePosition basePos) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
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
        // ✅ 打乱顺序，避免总是按相同顺序遍历
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

    // ==================== 辅助方法 ====================

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
     * 判断位置是否在ChokePoint方向的扇形区域内
     */
    private static boolean isInChokePointDirection(TilePosition pos, TilePosition base, TilePosition chokePoint, double angleThreshold) {
        if (chokePoint == null) {
            return false;
        }

        double chokeDx = chokePoint.getX() - base.getX();
        double chokeDy = chokePoint.getY() - base.getY();
        double chokeAngle = Math.atan2(chokeDy, chokeDx);

        double posDx = pos.getX() - base.getX();
        double posDy = pos.getY() - base.getY();
        double posAngle = Math.atan2(posDy, posDx);

        double angleDiff = Math.abs(normalizeAngle(chokeAngle - posAngle));
        return angleDiff <= Math.toRadians(angleThreshold / 2);
    }

    /**
     * 规范化角度到 [-π, π]
     */
    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * 获取最近的ChokePoint
     */
    private static TilePosition getNearestChokePoint(TilePosition position) {
        try {
            List<ChokePoint> chokePoints = Games.bwem.getMap().getChokePoints();
            if (chokePoints == null || chokePoints.isEmpty()) {
                return null;
            }

            ChokePoint nearest = chokePoints.stream()
                    .filter(cp -> cp != null && cp.getCenter() != null)
                    .min(Comparator.comparingInt(cp ->
                            cp.getCenter().getApproxDistance(position.toWalkPosition())))
                    .orElse(null);

            return nearest != null ? nearest.getCenter().toTilePosition() : null;
        } catch (Exception e) {
            System.err.println("获取路口信息失败: " + e.getMessage());
            return null;
        }
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

}
