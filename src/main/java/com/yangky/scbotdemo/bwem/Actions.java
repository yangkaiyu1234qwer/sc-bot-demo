package com.yangky.scbotdemo.bwem;

import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

import java.util.HashMap;
import java.util.Map;

/**
 * 单位动作封装类
 * 提供智能移动、绕行等高级动作
 */
public class Actions {
    private static final int STUCK_THRESHOLD = 30;
    public static final int DETOUR_DISTANCE = 96;

    private static final Map<Integer, Position> lastPositions = new HashMap<>();
    private static final Map<Integer, Integer> stuckFrames = new HashMap<>();


    /**
     * 智能移动到目标位置（自动避开气矿、建筑等障碍）
     *
     * @param unit   要移动的单位
     * @param target 目标位置
     * @return 是否成功下达移动命令
     */
    public static boolean smartMove(Unit unit, Position target) {
        if (unit == null || target == null) {
            return false;
        }

        if (checkStuck(unit, target)) {
            System.out.println("[DEBUG] 检测到工人卡住，执行强制脱离: " + unit.getID());
            forceBreakFree(unit, target);
            return true;
        }

        Unit blockingGeyser = findBlockingGeyser(unit, target);
        if (blockingGeyser != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingGeyser.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 检测到气矿阻挡，绕行至: " + detourPos);
                return unit.rightClick(detourPos);
            }
        }

        Unit blockingBuilding = findBlockingBuilding(unit, target);
        if (blockingBuilding != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingBuilding.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 检测到建筑阻挡，绕行至: " + detourPos +
                        " (Tile: " + new TilePosition(detourPos.getX()/32, detourPos.getY()/32) + ")");
                return unit.rightClick(detourPos);
            }
        }

        updateLastPosition(unit);
        return unit.rightClick(target);
    }

    /**
     * 智能移动到目标地块位置
     */
    public static boolean smartMove(Unit unit, TilePosition target) {
        if (target == null) {
            return false;
        }
        return smartMove(unit, target.toPosition());
    }

    /**
     * 查找是否有气矿挡住了路径
     */
    private static Unit findBlockingGeyser(Unit unit, Position target) {
        Position unitPos = unit.getPosition();
        return Games.game.getAllUnits().stream()
                .filter(u -> u.getType() == UnitType.Resource_Vespene_Geyser)
                .filter(u -> isOnPath(unitPos, u.getPosition(), target, 64))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找是否有建筑挡住了路径
     */
    private static Unit findBlockingBuilding(Unit unit, Position target) {
        Position unitPos = unit.getPosition();
        return Games.game.getAllUnits().stream()
                .filter(u -> u.getType().isBuilding())
                .filter(u -> u.isCompleted() || u.isBeingConstructed())
                .filter(u -> isOnPath(unitPos, u.getPosition(), target, 48))
                .findFirst()
                .orElse(null);
    }

    private static boolean isValidPosition(Position pos) {
        if (pos == null) {
            return false;
        }
        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;
        return pos.getX() >= 0 && pos.getX() <= mapWidth
                && pos.getY() >= 0 && pos.getY() <= mapHeight;
    }

    /**
     * 检查点是否在直线路径上（带容差）
     */
    public static boolean isOnPath(Position start, Position point, Position end, double tolerance) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return false;
        double dot = ((point.getX() - start.getX()) * dx + (point.getY() - start.getY()) * dy) / length;
        if (dot < 0 || dot > length) return false;
        double closestX = start.getX() + (dx / length) * dot;
        double closestY = start.getY() + (dy / length) * dot;
        double distance = Math.sqrt(
                Math.pow(point.getX() - closestX, 2) +
                        Math.pow(point.getY() - closestY, 2)
        );
        return distance <= tolerance;
    }

    private static boolean checkStuck(Unit unit, Position target) {
        int unitId = unit.getID();
        Position currentPos = unit.getPosition();

        if (!lastPositions.containsKey(unitId)) {
            lastPositions.put(unitId, currentPos);
            stuckFrames.put(unitId, 0);
            return false;
        }

        Position lastPos = lastPositions.get(unitId);
        double distance = currentPos.getDistance(lastPos);

        if (distance < 8) {
            int frames = stuckFrames.getOrDefault(unitId, 0) + 1;
            stuckFrames.put(unitId, frames);

            if (frames >= STUCK_THRESHOLD) {
                System.out.println("[DEBUG] 工人已卡住 " + frames + " 帧，当前位置: " + currentPos);
                return true;
            }
        } else {
            stuckFrames.put(unitId, 0);
        }

        lastPositions.put(unitId, currentPos);
        return false;
    }

    private static Position calculateDetourPosition(Position currentPos, Position obstaclePos, Position target) {
        double toTargetX = target.getX() - currentPos.getX();
        double toTargetY = target.getY() - currentPos.getY();

        double dx = currentPos.getX() - obstaclePos.getX();
        double dy = currentPos.getY() - obstaclePos.getY();

        int detourX, detourY;

        if (Math.abs(dx) > Math.abs(dy)) {
            detourY = toTargetY >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
            detourX = 0;
        } else {
            detourX = toTargetX >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
            detourY = 0;
        }

        int newX = obstaclePos.getX() + detourX;
        int newY = obstaclePos.getY() + detourY;

        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;

        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));

        return new Position(newX, newY);
    }

    private static void forceBreakFree(Unit unit, Position target) {
        int unitId = unit.getID();
        stuckFrames.put(unitId, 0);

        unit.stop();

        Position currentPos = unit.getPosition();
        double angle = Math.random() * 2 * Math.PI;
        int breakDistance = 128;

        int newX = (int) (currentPos.getX() + Math.cos(angle) * breakDistance);
        int newY = (int) (currentPos.getY() + Math.sin(angle) * breakDistance);

        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;

        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));

        Position breakPos = new Position(newX, newY);
        System.out.println("[DEBUG] 随机方向脱离至: " + breakPos);
        unit.rightClick(breakPos);
    }

    private static void updateLastPosition(Unit unit) {
        lastPositions.put(unit.getID(), unit.getPosition());
        stuckFrames.put(unit.getID(), 0);
    }

    public static void forceMove(Unit unit, Position target) {
        if (unit == null || target == null) {
            return;
        }
        unit.stop();

        Unit blockingUnit = findBlockingBuilding(unit, target);
        if (blockingUnit == null) {
            blockingUnit = findBlockingGeyser(unit, target);
        }
        if (blockingUnit != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingUnit.getPosition(), target);
            if (isValidPosition(detourPos)) {
                unit.rightClick(detourPos);
                return;
            }
        }

        unit.rightClick(target);
    }

    /**
     * 返回基地放下资源
     */
    public static boolean returnCargo(Unit unit) {
        if (unit == null) {
            return false;
        }
        if (!unit.isCarryingMinerals() && !unit.isCarryingGas()) {
            return true;
        }
        Unit base = Bases.getNearestBase(unit.getTilePosition());
        if (base != null) {
            return unit.returnCargo();
        }
        return false;
    }

    public static void clearWorkerTracking() {
        lastPositions.clear();
        stuckFrames.clear();
    }
}
