package com.yangky.scbotdemo.bwem.region;


import bwapi.Color;
import bwapi.Position;
import bwapi.TilePosition;
import bwem.ChokePoint;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.walloff.WallOff;
import com.yangky.scbotdemo.bwem.walloff.WallOffBuilding;
import com.yangky.scbotdemo.bwem.walloff.WallOffConfig;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class RegionsClassifier {

    private static final int CHOKE_POINT_EXPAND_DISTANCE = 4;
    private static final boolean DEBUG_ENABLED = true;  // ✅ 总调试开关：控制日志和绘图
    private static boolean initialized = false;
    @Getter
    private static Set<RegionPosition> positions = new CopyOnWriteArraySet<>();
    private static final Set<TilePosition> chokePointSet = new CopyOnWriteArraySet<>();
    private static final Set<TilePosition> centralPositionSet = new CopyOnWriteArraySet<>();
    @Getter
    private static TilePosition centralPosition;
    @Getter
    private static TilePosition chokePointPosition;

    public static void initialize() {
        if (initialized) {
            // 绘制区域标记
            if (DEBUG_ENABLED) {
                positions.forEach(RegionsClassifier::drawRegionMarker);
            }
            return;
        }
        TilePosition startLocation = Games.game.self().getStartLocation();
        Map<TilePosition, WallOff> map = WallOffConfig.getMap();
        if (map != null && !map.isEmpty()) {
            List<WallOffBuilding> wallOffs = map.entrySet().stream()
                    .min(Comparator.comparing(entry -> getPathDistance(entry.getKey(), startLocation)))
                    .map(Map.Entry::getValue)
                    .map(WallOff::getList)
                    .orElse(new ArrayList<>());
            wallOffs = new ArrayList<>(wallOffs);
            if (wallOffs.stream().anyMatch(e -> e.getIdempotentNo().contains("secondBarrack"))) {
                wallOffs.removeIf(e -> e.getIdempotentNo().contains("firstBarrack"));
            }
            chokePointSet.addAll(wallOffs.stream()
                    .map(WallOffBuilding::getTilePosition)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        } else {
            List<ChokePoint> chokePoints = Bases.getMainBase().getArea().getChokePoints();
            chokePointSet.addAll(chokePoints.stream()
                    .map(e -> e.getCenter().toTilePosition())
                    .collect(Collectors.toSet()));
        }
        Set<TilePosition> expandedChokePoints = expandChokePoints(chokePointSet);
        // 绘制路口标记
        if (DEBUG_ENABLED) {
            chokePointSet.forEach(pos -> {
                if (pos != null) {
                    Games.game.drawCircleMap(pos.toPosition(), 50, new Color(255, 0, 0));
                }
            });
        }
        floodFillFromBase(startLocation, expandedChokePoints);
        initialized = true;
    }

    public static void floodFillFromBase(TilePosition basePos, Set<TilePosition> chokePoints) {
        if (DEBUG_ENABLED) {
            System.out.println("[RegionClassifier] BFS起点: " + basePos + ", 路口数: " + chokePoints.size());
        }
        Set<RegionPosition> result = new HashSet<>();
        Queue<TilePosition> queue = new LinkedList<>();
        Set<TilePosition> visited = new HashSet<>();
        Set<TilePosition> reachableArea = new HashSet<>();
        queue.add(basePos);
        visited.add(basePos);
        // BFS 找出所有可达区域
        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            reachableArea.add(current);
            for (TilePosition neighbor : getAllNeighbors(current)) {
                if (visited.contains(neighbor) || !isInMap(neighbor) || chokePoints.contains(neighbor)) {
                    continue;
                }
                if (Games.isBuildable(neighbor) || RegionPosition.isMineralOrGas(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        if (DEBUG_ENABLED) {
            System.out.println("[RegionClassifier] 可达区域: " + reachableArea.size() + " 格");
            if (!reachableArea.isEmpty()) {
                int minX = reachableArea.stream().mapToInt(TilePosition::getX).min().orElse(0);
                int maxX = reachableArea.stream().mapToInt(TilePosition::getX).max().orElse(0);
                int minY = reachableArea.stream().mapToInt(TilePosition::getY).min().orElse(0);
                int maxY = reachableArea.stream().mapToInt(TilePosition::getY).max().orElse(0);
                System.out.println("[RegionClassifier] 范围: X[" + minX + "," + maxX + "], Y[" + minY + "," + maxY + "]");
            }
        }
        // 预先计算边界点集合
        Set<TilePosition> boundarySet = identifyBoundary(reachableArea);
        // 获取基地位置和矿物列表
        List<bwapi.Unit> minerals = getNearbyMinerals(basePos);
        // 对可达区域进行分类
        for (TilePosition pos : reachableArea) {
            RegionType type = classifyPosition(pos, basePos, minerals, boundarySet);
            RegionPosition regionPos = new RegionPosition();
            regionPos.setX(pos.getX());
            regionPos.setY(pos.getY());
            regionPos.setType(type);
            regionPos.setBuildable(Games.game.isBuildable(pos));
            regionPos.mineralOrGasMark(pos);
            regionPos.setChokePoint(chokePoints.contains(pos));
            result.add(regionPos);
        }
        positions = result;
        calculateCentralAreaCenter();
        calculateChokePointAreaCenter();
    }

    /**
     * 绘制区域标记
     */
    private static void drawRegionMarker(RegionPosition pos) {
        Position pixelPos = new TilePosition(pos.getX(), pos.getY()).toPosition();
        switch (pos.getType()) {
            case MINERAL:
                // 蓝色圆圈 - 矿区
                Games.game.drawCircleMap(pixelPos, 6, new Color(0, 0, 255));
                break;
            case EDGE:
                // 黄色三角形 - 外圈
                drawTriangle(pixelPos, 6, new Color(255, 255, 0));
                break;
            case CENTRAL:
                // 绿色小圆点 - 核心圈
                Games.game.drawCircleMap(pixelPos, 4, new Color(0, 255, 0));
                break;
            case BOUNDARY:
                // 橙色X - 边界
                drawXMark(pixelPos, 8, new Color(255, 165, 0));
                break;
            default:
                break;
        }
        drawXMark(centralPosition.toPosition(), 8, new Color(255, 0, 0));
        drawXMark(chokePointPosition.toPosition(), 8, new Color(255, 0, 0));
    }

    /**
     * 识别边界点
     */
    private static Set<TilePosition> identifyBoundary(Set<TilePosition> reachableArea) {
        Set<TilePosition> boundary = new HashSet<>();
        for (TilePosition pos : reachableArea) {
            if (isBoundary(pos, reachableArea, Collections.emptySet())) {
                boundary.add(pos);
            }
        }
        return boundary;
    }

    /**
     * 获取附近的矿物和气矿
     */
    private static List<bwapi.Unit> getNearbyMinerals(TilePosition basePos) {
        List<bwapi.Unit> resources = new ArrayList<>();
        // 扩大搜索范围到基地周围10格
        int searchRadius = 10;
        int minX = Math.max(0, basePos.getX() - searchRadius);
        int maxX = Math.min(Games.game.mapWidth() - 1, basePos.getX() + searchRadius);
        int minY = Math.max(0, basePos.getY() - searchRadius);
        int maxY = Math.min(Games.game.mapHeight() - 1, basePos.getY() + searchRadius);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                TilePosition pos = new TilePosition(x, y);
                for (bwapi.Unit unit : Games.game.getUnitsOnTile(pos)) {
                    if (unit.getType().isResourceContainer() && !resources.contains(unit)) {
                        resources.add(unit);
                    }
                }
            }
        }
        return resources;
    }

    /**
     * 对位置进行分类
     */
    private static RegionType classifyPosition(TilePosition pos, TilePosition basePos,
                                               List<bwapi.Unit> minerals,
                                               Set<TilePosition> boundarySet) {
        if (boundarySet.contains(pos)) {
            return RegionType.BOUNDARY;
        }
        // 1. 检查是否是矿区（矿物/气矿1格内 或 基地3格内）
        if (isMineralZone(pos, minerals, basePos)) {
            return RegionType.MINERAL;
        }
        // 2. 检查是否是外圈（距离边界2格内）
        if (calculateDistanceToBoundary(pos, boundarySet) <= 3) {
            return RegionType.EDGE;
        }
        // 3. 核心圈（可建造的位置）
        if (Games.isBuildable(pos)) {
            centralPositionSet.add(pos);
            return RegionType.CENTRAL;
        }
        // 4. 其他不可建造的位置归为边界
        return RegionType.BOUNDARY;
    }

    /**
     * 检查是否是矿区
     */
    private static boolean isMineralZone(TilePosition pos, List<bwapi.Unit> minerals, TilePosition basePos) {
        // 检查是否在基地5格内
        if (pos.getApproxDistance(basePos) <= 5) {
            return true;
        }
        // 检查是否在矿物/气矿1格内
        for (bwapi.Unit mineral : minerals) {
            TilePosition mineralPos = mineral.getTilePosition();
            if (pos.getApproxDistance(mineralPos) <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算到边界的距离
     */
    private static int calculateDistanceToBoundary(TilePosition pos, Set<TilePosition> boundarySet) {
        if (boundarySet.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int minDist = Integer.MAX_VALUE;
        for (TilePosition boundary : boundarySet) {
            int dist = pos.getApproxDistance(boundary);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }


    /**
     * 判断是否是边界点
     */
    private static boolean isBoundary(TilePosition pos, Set<TilePosition> reachableArea, Set<TilePosition> chokePoints) {
        for (TilePosition neighbor : getAllNeighbors(pos)) {
            if (!isInMap(neighbor) || chokePoints.contains(neighbor) || !reachableArea.contains(neighbor)) {
                return true;
            }
        }
        return false;
    }

    private static List<TilePosition> getNeighbors(TilePosition pos) {
        List<TilePosition> neighbors = new ArrayList<>(4);
        int[][] dirs = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        for (int[] dir : dirs) {
            neighbors.add(new TilePosition(pos.getX() + dir[0], pos.getY() + dir[1]));
        }
        return neighbors;
    }

    private static List<TilePosition> getAllNeighbors(TilePosition pos) {
        List<TilePosition> neighbors = new ArrayList<>(8);
        int[][] dirs = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
        for (int[] dir : dirs) {
            neighbors.add(new TilePosition(pos.getX() + dir[0], pos.getY() + dir[1]));
        }
        return neighbors;
    }

    private static boolean isInMap(TilePosition pos) {
        return pos.getX() >= 0 && pos.getY() >= 0 &&
                pos.getX() < Games.game.mapWidth() && pos.getY() < Games.game.mapHeight();
    }

    private static void drawXMark(Position center, int size, Color color) {
        int halfSize = size / 2;
        Games.game.drawLineMap(
                center.getX() - halfSize, center.getY() - halfSize,
                center.getX() + halfSize, center.getY() + halfSize, color);
        Games.game.drawLineMap(
                center.getX() - halfSize, center.getY() + halfSize,
                center.getX() + halfSize, center.getY() - halfSize, color);
    }

    /**
     * 绘制三角形
     */
    private static void drawTriangle(Position center, int size, Color color) {
        int x1 = center.getX();
        int y1 = center.getY() - size;
        int x2 = center.getX() - size;
        int y2 = center.getY() + size;
        int x3 = center.getX() + size;
        int y3 = center.getY() + size;
        Games.game.drawLineMap(x1, y1, x2, y2, color);
        Games.game.drawLineMap(x2, y2, x3, y3, color);
        Games.game.drawLineMap(x3, y3, x1, y1, color);
    }

    /**
     * BFS 计算最短路径距离
     */
    private static int getPathDistance(TilePosition start, TilePosition end) {
        if (start.equals(end)) return 0;
        Set<TilePosition> visited = new HashSet<>();
        Queue<TilePosition> queue = new LinkedList<>();
        Map<TilePosition, Integer> distMap = new HashMap<>();
        queue.add(start);
        visited.add(start);
        distMap.put(start, 0);
        while (!queue.isEmpty()) {
            TilePosition current = queue.poll();
            int currentDist = distMap.get(current);
            for (TilePosition neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor) || !isInMap(neighbor)) continue;
                if (!Games.isBuildable(neighbor) && !RegionPosition.isMineralOrGas(neighbor)) continue;
                int newDist = currentDist + 1;
                if (neighbor.equals(end)) {
                    return newDist;
                }
                visited.add(neighbor);
                queue.add(neighbor);
                distMap.put(neighbor, newDist);
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 膨胀路口：距离任意路口点 <= 4 的格子都算作屏障
     */
    private static Set<TilePosition> expandChokePoints(Set<TilePosition> originalChokePoints) {
        if (originalChokePoints.isEmpty()) {
            return originalChokePoints;
        }
        Set<TilePosition> expanded = new HashSet<>();
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                for (TilePosition cp : originalChokePoints) {
                    if (Math.abs(cp.getX() - x) + Math.abs(cp.getY() - y) <= CHOKE_POINT_EXPAND_DISTANCE) {
                        expanded.add(new TilePosition(x, y));
                        break;
                    }
                }
            }
        }
        if (DEBUG_ENABLED) {
            System.out.println("[RegionClassifier] 路口膨胀: 原始=" + originalChokePoints.size()
                    + ", 膨胀后=" + expanded.size() + " (距离=" + CHOKE_POINT_EXPAND_DISTANCE + ")");
        }
        return expanded;
    }

    private static void calculateCentralAreaCenter() {
        if (centralPositionSet.isEmpty()) {
            System.out.println("[Locations] 警告：CENTRAL 区域为空");
            return;
        }
        // 取所有 CENTRAL 区域的 Tile 的平均值
        int totalX = 0;
        int totalY = 0;
        int count = 0;
        for (TilePosition pos : centralPositionSet) {
            totalX += pos.getX();
            totalY += pos.getY();
            count++;
        }
        if (count == 0) {
            return;
        }
        centralPosition = new TilePosition(totalX / count, totalY / count);
        System.out.println("[Locations] CENTRAL 中心点: " + centralPosition);
    }

    private static void calculateChokePointAreaCenter() {
        if (chokePointSet.isEmpty()) {
            System.out.println("[Locations] 警告：CHOKE POINT 区域为空");
            return;
        }
        // 取所有 CENTRAL 区域的 Tile 的平均值
        int totalX = 0;
        int totalY = 0;
        int count = 0;
        for (TilePosition pos : chokePointSet) {
            totalX += pos.getX();
            totalY += pos.getY();
            count++;
        }
        if (count == 0) {
            return;
        }
        chokePointPosition = new TilePosition(totalX / count, totalY / count);
        System.out.println("[Locations] CHOKE POINT 中心点: " + chokePointPosition);
    }

    public static void clear() {
        positions.clear();
        centralPositionSet.clear();
    }

    public static TilePosition getChokePointPosition() {
        return chokePointPosition;
    }
}

