package com.yangky.scbotdemo.bwem;

import bwapi.TilePosition;
import bwapi.Unit;
import bwem.Area;
import bwem.Base;
import bwem.ChokePoint;
import bwem.Mineral;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 地图区域分析工具类
 * 游戏启动时一次性计算出生区域的边界和分区
 * <p>
 * 区域划分：
 * 1. MINERAL - 矿区（基地周围5格 + 矿附近2格）
 * 2. CHOKE_POINT - 路口/出口附近（ChokePoint ± 3格）
 * 3. EDGE - 边缘（距离边界3格内的可建造区域）
 * 4. CENTRAL - 腹地（内部可建造区域）
 * 5. BOUNDARY - 边界本身（连续的不可建造地形）
 *
 * @author yangky
 * @Date 2026/5/2 12:24
 */
public class Locations {

    private static final boolean DEBUG_LOG = true;
    private static final int EDGE_DISTANCE = 3;

    private static boolean initialized = false;
    @Getter
    private static final Map<TilePosition, Location> locationMap = new HashMap<>();
    @Getter
    private static Set<TilePosition> outerBoundary = new HashSet<>();
    private static List<ChokePoint> exits = new ArrayList<>();
    private static Set<TilePosition> reachableArea = new HashSet<>();


    /**
     * 初始化地图区域分析
     */
    public static void initialize() {
        if (initialized) {
            log("[Locations] 已初始化，跳过");
            return;
        }
        long startTime = System.currentTimeMillis();
        log("[Locations] 开始地图区域分析...");

        Unit mainBaseUnit = Bases.getMainBaseUnit();
        if (mainBaseUnit == null) {
            log("[Locations] 错误：未找到主基地");
            return;
        }
        Base mainBase = Bases.getBaseFormBaseUnit(mainBaseUnit);
        reachableArea = floodFillFromBase(mainBase.getLocation());
        log("[Locations] 可达区域大小: " + reachableArea.size());
        outerBoundary = identifyOuterBoundary(reachableArea);
        log("[Locations] 边界大小: " + outerBoundary.size());
        exits = identifyExits();
        log("[Locations] 出口数量: " + exits.size());
        classifyAllPositions(reachableArea, mainBase);
        initialized = true;
        long elapsed = System.currentTimeMillis() - startTime;
        log("[Locations] 分析完成！耗时: " + elapsed + "ms, " +
                "可达区域: " + reachableArea.size() + " 格, " +
                "边界: " + outerBoundary.size() + " 格, " +
                "出口: " + exits.size() + " 个");
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取指定位置的分类信息
     */
    public static Location getLocation(TilePosition pos) {
        return locationMap.get(pos);
    }

    /**
     * 获取指定区域类型的所有位置
     */
    public static Set<TilePosition> getPositionsByRegion(Location.RegionType type) {
        Set<TilePosition> result = new CopyOnWriteArraySet<>();
        for (Map.Entry<TilePosition, Location> entry : locationMap.entrySet()) {
            if (entry.getValue().getRegionType() == type) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 洪水填充：从基地出发获取所有可达位置
     */
    private static Set<TilePosition> floodFillFromBase(TilePosition basePos) {
        Set<TilePosition> visited = new HashSet<>();
        Queue<TilePosition> queue = new LinkedList<>();
        queue.add(basePos);
        visited.add(basePos);
        int maxSearchRadius = 40;
        Base mainBase = Bases.getBaseFormBaseUnit(Bases.getMainBaseUnit());
        // 获取主基地所属区域
        Area mainArea = mainBase.getArea();
        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            if (current.getApproxDistance(basePos) > maxSearchRadius) {
                continue;
            }
            for (TilePosition neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor) && isInMap(neighbor)) {
                    // 新增：检查邻居是否属于主基地的 Area
                    Area neighborArea = Games.bwem.getMap().getArea(neighbor);
                    if (neighborArea == null || !neighborArea.equals(mainArea)) {
                        // 不属于主矿区域
                        continue;
                    }
                    if (Games.isBuildable(neighbor) || isMineralOrGeyser(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return visited;
    }

    /**
     * 识别外围边界
     */
    private static Set<TilePosition> identifyOuterBoundary(Set<TilePosition> area) {
        Set<TilePosition> boundary = new HashSet<>();
        for (TilePosition pos : area) {
            for (TilePosition neighbor : getAllNeighbors(pos)) {
                if (!area.contains(neighbor)) {
                    if (!isInMap(neighbor) || (!Games.isBuildable(neighbor) && !isMineralOrGeyser(neighbor))) {
                        boundary.add(pos);
                        break;
                    }
                }
            }
        }
        return boundary;
    }

    /**
     * 识别出口（ChokePoint）
     */
    private static List<ChokePoint> identifyExits() {
        List<ChokePoint> result = new ArrayList<>();
        try {
            List<ChokePoint> allChokePoints = Games.bwem.getMap().getChokePoints();
            if (allChokePoints == null || allChokePoints.isEmpty()) {
                return result;
            }
            Base mainBase = Bases.getMainBase();
            if (mainBase == null) {
                return result;
            }
            TilePosition basePos = mainBase.getLocation();
            for (ChokePoint cp : allChokePoints) {
                if (cp != null && cp.getCenter() != null) {
                    TilePosition chokeTile = cp.getCenter().toTilePosition();
                    int distance = chokeTile.getApproxDistance(basePos);

                    if (distance < 30) {
                        result.add(cp);
                    }
                }
            }
        } catch (Exception e) {
            log("[Locations] 识别出口失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 对所有位置进行分类
     */
    private static void classifyAllPositions(Set<TilePosition> area, Base mainBase) {
        TilePosition basePos = mainBase.getLocation();
        List<Mineral> minerals = mainBase.getMinerals();
        for (TilePosition pos : area) {
            boolean isBuildable = Games.isBuildable(pos);
            Location loc = new Location(pos, isBuildable);
            int distToBase = pos.getApproxDistance(basePos);
            loc.setDistanceToBase(distToBase);
            double distToMineral = calculateDistanceToNearestMineral(pos, minerals);
            loc.setDistanceToNearestMineral(distToMineral);
            ChokePoint nearestCp = findNearestChokePoint(pos);
            loc.setNearestChokePoint(nearestCp);
            int distToBoundary = calculateDistanceToBoundary(pos);
            loc.setDistanceToBoundary(distToBoundary);
            Location.RegionType type = classifyPosition(loc, distToBase, distToMineral, nearestCp, distToBoundary);
            loc.setRegionType(type);

            locationMap.put(pos, loc);
        }
    }
// ... existing code ...

    /**
     * 分类单个位置
     */
    private static Location.RegionType classifyPosition(Location loc, int distToBase,
                                                        double distToMineral,
                                                        ChokePoint nearestCp,
                                                        int distToBoundary) {
        if (!loc.isBuildable()) {
            return Location.RegionType.BOUNDARY;
        }
        if (distToMineral <= 2 || distToBase <= 5) {
            return Location.RegionType.MINERAL;
        }
        if (nearestCp != null) {
            TilePosition basePos = Bases.getMainBase().getLocation();
            TilePosition chokeTile = nearestCp.getCenter().toTilePosition();
            int distFromBaseToChoke = basePos.getApproxDistance(chokeTile);

            if (Math.abs(distToBase - distFromBaseToChoke) <= 3) {
                return Location.RegionType.CHOKE_POINT;
            }
        }
        if (distToBoundary <= EDGE_DISTANCE) {
            return Location.RegionType.EDGE;
        }
        return Location.RegionType.CENTRAL;
    }

    /**
     * 计算到最近矿的距离
     */
    private static double calculateDistanceToNearestMineral(TilePosition pos, List<Mineral> minerals) {
        if (minerals == null || minerals.isEmpty()) {
            return Double.MAX_VALUE;
        }
        double minDistance = Double.MAX_VALUE;
        for (Mineral mineral : minerals) {
            if (mineral != null && mineral.getUnit().getPosition() != null) {
                TilePosition mineralPos = mineral.getUnit().getTilePosition();
                double distance = pos.getApproxDistance(mineralPos);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    /**
     * 查找最近的ChokePoint
     */
    private static ChokePoint findNearestChokePoint(TilePosition pos) {
        if (exits == null || exits.isEmpty()) {
            return null;
        }
        ChokePoint nearest = null;
        int minDistance = Integer.MAX_VALUE;
        for (ChokePoint cp : exits) {
            if (cp != null && cp.getCenter() != null) {
                TilePosition chokeTile = cp.getCenter().toTilePosition();
                int distance = pos.getApproxDistance(chokeTile);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = cp;
                }
            }
        }
        return nearest;
    }

    /**
     * 计算到边界的距离
     */
    private static int calculateDistanceToBoundary(TilePosition pos) {
        if (outerBoundary == null || outerBoundary.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int minDistance = Integer.MAX_VALUE;
        for (TilePosition boundaryPos : outerBoundary) {
            int distance = pos.getApproxDistance(boundaryPos);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    /**
     * 获取4个方向的邻居
     */
    private static List<TilePosition> getNeighbors(TilePosition pos) {
        List<TilePosition> neighbors = new ArrayList<>();
        int[][] directions = {
                {0, -1},
                {-1, 0}, {1, 0},
                {0, 1}
        };

        for (int[] dir : directions) {
            TilePosition neighbor = new TilePosition(pos.getX() + dir[0], pos.getY() + dir[1]);
            neighbors.add(neighbor);
        }
        return neighbors;
    }

    /**
     * 获取8个方向的邻居
     */
    private static List<TilePosition> getAllNeighbors(TilePosition pos) {
        List<TilePosition> neighbors = new ArrayList<>();
        int[][] directions = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0}, {1, 0},
                {-1, 1}, {0, 1}, {1, 1}
        };
        for (int[] dir : directions) {
            TilePosition neighbor = new TilePosition(pos.getX() + dir[0], pos.getY() + dir[1]);
            neighbors.add(neighbor);
        }
        return neighbors;
    }
// ... existing code ...


    /**
     * 检查位置是否在地图内
     */
    private static boolean isInMap(TilePosition pos) {
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        return pos.getX() >= 0 && pos.getY() >= 0 &&
                pos.getX() < mapWidth && pos.getY() < mapHeight;
    }

    /**
     * 检查是否是矿或气矿
     */
    private static boolean isMineralOrGeyser(TilePosition pos) {
        List<bwapi.Unit> units = Games.game.getUnitsOnTile(pos);
        for (bwapi.Unit unit : units) {
            bwapi.UnitType type = unit.getType();
            if (type == bwapi.UnitType.Resource_Mineral_Field ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_2 ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_3 ||
                    type == bwapi.UnitType.Resource_Vespene_Geyser) {
                return true;
            }
        }
        return false;
    }
// ... existing code ...


    /**
     * 日志输出
     */
    private static void log(String message) {
        if (DEBUG_LOG) {
            System.out.println(message);
        }
    }
}
