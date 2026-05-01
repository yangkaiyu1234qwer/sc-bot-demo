package com.yangky.scbotdemo.bwem;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PathValidator - 位置可达性验证工具类
 * 用于判断指定位置是否可以到达，以及位置的合法性检查
 *
 * @author yangky
 * @Date 2026/4/30
 */
public class PathValidator {

    private static final MapCache<Boolean> walkableCache = new MapCache<>(300000);
    private static final MapCache<Boolean> reachableCache = new MapCache<>(60000);
    private static Long lastMapResetTime = 0L;

    /**
     * 判断目标位置是否从起始位置可达
     * 使用BFS算法进行路径搜索
     *
     * @param startUnit  起始单位（工人）
     * @param targetTile 目标地块位置
     * @return 是否可达
     */
    public static boolean isReachable(Unit startUnit, TilePosition targetTile) {
        if (startUnit == null || !startUnit.exists()) {
            return false;
        }

        if (targetTile == null) {
            return false;
        }

        Position startPos = startUnit.getPosition();
        Position targetPos = targetTile.toPosition();

        String cacheKey = startPos.getX() + "," + startPos.getY() + "->" + targetTile.getX() + "," + targetTile.getY();

        Boolean cached = reachableCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        if (startPos.getDistance(targetPos) < 64) {
            return true;
        }

        boolean result = bfsPathfinding(startPos, targetPos);
        reachableCache.put(cacheKey, result);

        return result;
    }

    /**
     * 判断目标位置是否合法（在地图范围内且可建造）
     *
     * @param tile           待检查的地块位置
     * @param buildingWidth  建筑宽度（地块单位）
     * @param buildingHeight 建筑高度（地块单位）
     * @return 是否合法
     */
    public static boolean isValidBuildLocation(TilePosition tile, int buildingWidth, int buildingHeight) {
        if (tile == null) {
            return false;
        }

        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        if (tile.getX() < 0 || tile.getY() < 0 ||
                tile.getX() + buildingWidth > mapWidth ||
                tile.getY() + buildingHeight > mapHeight) {
            return false;
        }

        for (int dx = 0; dx < buildingWidth; dx++) {
            for (int dy = 0; dy < buildingHeight; dy++) {
                TilePosition checkTile = new TilePosition(tile.getX() + dx, tile.getY() + dy);
                if (!isTileBuildable(checkTile)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 检查单个地块是否可建造（带缓存）
     */
    private static boolean isTileBuildable(TilePosition tile) {
        String key = tile.getX() + "," + tile.getY();

        Boolean cached = walkableCache.get(key);
        if (cached != null) {
            return cached;
        }

        boolean result = Games.game.isBuildable(tile);
        walkableCache.put(key, result);

        return result;
    }

    /**
     * 获取从起点到终点的近似距离（考虑地形障碍）
     *
     * @param startPos  起始位置
     * @param targetPos 目标位置
     * @return 估算的路径距离，如果不可达返回-1
     */
    public static double getApproxPathDistance(Position startPos, Position targetPos) {
        if (startPos == null || targetPos == null) {
            return -1;
        }

        double straightDistance = startPos.getDistance(targetPos);

        if (bfsPathfinding(startPos, targetPos)) {
            return straightDistance;
        }

        return -1;
    }

    /**
     * BFS路径搜索算法
     *
     * @param startPos  起始位置
     * @param targetPos 目标位置
     * @return 是否找到路径
     */
    private static boolean bfsPathfinding(Position startPos, Position targetPos) {
        TilePosition startTile = startPos.toTilePosition();
        TilePosition targetTile = targetPos.toTilePosition();

        Set<TilePosition> visited = new HashSet<>();
        Queue<TilePosition> queue = new LinkedList<>();

        queue.offer(startTile);
        visited.add(startTile);

        int maxSteps = 500;
        int steps = 0;

        while (!queue.isEmpty() && steps < maxSteps) {
            TilePosition current = queue.poll();
            steps++;

            if (current.equals(targetTile)) {
                return true;
            }

            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] dir : directions) {
                TilePosition next = new TilePosition(
                        current.getX() + dir[0],
                        current.getY() + dir[1]
                );

                if (visited.contains(next)) {
                    continue;
                }

                if (!isWalkable(next)) {
                    continue;
                }

                visited.add(next);
                queue.offer(next);
            }
        }

        return false;
    }

    /**
     * 检查地块是否可通行
     *
     * @param tile 待检查的地块
     * @return 是否可通行
     */
    private static boolean isWalkable(TilePosition tile) {
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();

        if (tile.getX() < 0 || tile.getY() < 0 ||
                tile.getX() >= mapWidth || tile.getY() >= mapHeight) {
            return false;
        }

        if (!Games.game.isBuildable(tile)) {
            return Games.game.isWalkable(tile.toWalkPosition());
        }

        return true;
    }

    /**
     * 打印位置调试信息
     *
     * @param tile        地块位置
     * @param description 描述信息
     */
    public static void printPositionDebugInfo(TilePosition tile, String description) {
        if (tile == null) {
            System.out.println("[位置调试] " + description + " - 位置为空");
            return;
        }
        System.out.println("[位置调试] " + description);
        System.out.println("  - 地块坐标: (" + tile.getX() + ", " + tile.getY() + ")");
        System.out.println("  - 像素坐标: (" + tile.toPosition().getX() + ", " + tile.toPosition().getY() + ")");
        System.out.println("  - 是否可建造: " + Games.game.isBuildable(tile));
        System.out.println("  - 是否可行走: " + Games.game.isWalkable(tile.toWalkPosition()));
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        System.out.println("  - 地图范围: [0-" + mapWidth + ", 0-" + mapHeight + "]");
        System.out.println("  - 是否在地图内: " +
                (tile.getX() >= 0 && tile.getY() >= 0 && tile.getX() < mapWidth && tile.getY() < mapHeight));
    }

    /**
     * 重置缓存（新游戏开始时调用）
     */
    public static void resetCache() {
        walkableCache.clear();
        reachableCache.clear();
        lastMapResetTime = System.currentTimeMillis();
    }

    /**
     * 简单的内存缓存
     */
    private static class MapCache<V> {
        private final ConcurrentHashMap<String, V> cache = new ConcurrentHashMap<>();
        private final long ttlMillis;
        private final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();

        public MapCache(long ttlMillis) {
            this.ttlMillis = ttlMillis;
        }

        public V get(String key) {
            Long timestamp = timestamps.get(key);
            if (timestamp == null) {
                return null;
            }
            if (System.currentTimeMillis() - timestamp > ttlMillis) {
                cache.remove(key);
                timestamps.remove(key);
                return null;
            }
            return cache.get(key);
        }

        public void put(String key, V value) {
            cache.put(key, value);
            timestamps.put(key, System.currentTimeMillis());
        }

        public void clear() {
            cache.clear();
            timestamps.clear();
        }
    }
}