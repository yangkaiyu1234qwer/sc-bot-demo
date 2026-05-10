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
            forceBreakFree(unit);
            return true;
        }

        Unit blockingGeyser = findBlockingGeyser(unit, target);
        if (blockingGeyser != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingGeyser.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 检测到气矿" + blockingGeyser.getTilePosition() + "阻挡，绕行至: " + detourPos.toTilePosition());
                return unit.rightClick(detourPos);
            }
        }

        Unit blockingBuilding = findBlockingBuilding(unit, target);
        if (blockingBuilding != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingBuilding.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 检测到建筑" + blockingBuilding.getTilePosition() + "阻挡 建筑类型=" + blockingBuilding.getType() + ", position=" + target.toTilePosition() + "，绕行至: " + detourPos.toTilePosition());
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

        // ✅ 尝试两个绕行方向：横向绕行和纵向绕行
        Position horizontalDetour = calculateHorizontalDetour(obstaclePos, toTargetX);
        Position verticalDetour = calculateVerticalDetour(obstaclePos, toTargetY);

        if (horizontalDetour == null && verticalDetour == null) {
            return null;
        }

        if (horizontalDetour == null) {
            return verticalDetour;
        }

        if (verticalDetour == null) {
            return horizontalDetour;
        }

        // ✅ 选择距离目标更近的绕行点
        double distToHorizontal = horizontalDetour.getDistance(target);
        double distToVertical = verticalDetour.getDistance(target);

        System.out.println("[DEBUG] 绕行方案对比 - 横向: " + horizontalDetour.toTilePosition()
                + " (距离:" + (int) distToHorizontal + "), 纵向: " + verticalDetour.toTilePosition()
                + " (距离:" + (int) distToVertical + ")");

        return distToHorizontal <= distToVertical ? horizontalDetour : verticalDetour;
    }

    /**
     * 计算横向绕行点（左右绕行）
     */
    private static Position calculateHorizontalDetour(Position obstaclePos, double toTargetX) {
        int detourX = toTargetX >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
        int newX = obstaclePos.getX() + detourX;
        int newY = obstaclePos.getY();

        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;

        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));

        Position pos = new Position(newX, newY);
        return isValidPosition(pos) ? pos : null;
    }

    /**
     * 计算纵向绕行点（上下绕行）
     */
    private static Position calculateVerticalDetour(Position obstaclePos, double toTargetY) {
        int detourY = toTargetY >= 0 ? DETOUR_DISTANCE : -DETOUR_DISTANCE;
        int newX = obstaclePos.getX();
        int newY = obstaclePos.getY() + detourY;

        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;

        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));

        Position pos = new Position(newX, newY);
        return isValidPosition(pos) ? pos : null;
    }


    public static void forceBreakFree(Unit unit) {
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

    /**
     * 清除指定工人的跟踪记录（用于建筑完成后重新移动）
     */
    public static void clearWorkerTracking(Unit unit) {
        if (unit != null) {
            lastPositions.remove(unit.getID());
            stuckFrames.remove(unit.getID());
        }
    }

    public static void clearWorkerTracking() {
        lastPositions.clear();
        stuckFrames.clear();
    }
}
