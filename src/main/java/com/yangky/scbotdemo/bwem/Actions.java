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
    private static final Map<Integer, java.util.Set<Position>> attemptedDetours = new HashMap<>();


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

        if (checkStuck(unit)) {
            System.out.println("[DEBUG] 检测到工人卡住，执行强制脱离: " + unit.getID());
            forceBreakFree(unit);
            return true;
        }

        // 只有靠近障碍物时才检测并绕行
        Unit blockingGeyser = findNearbyBlockingGeyser(unit, target);
        if (blockingGeyser != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingGeyser.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 靠近气矿" + blockingGeyser.getTilePosition() + "，绕行至: " + detourPos.toTilePosition());
                return unit.rightClick(detourPos);
            }
        }

        Unit blockingBuilding = findNearbyBlockingBuilding(unit, target);
        if (blockingBuilding != null) {
            Position detourPos = calculateDetourPosition(unit.getPosition(), blockingBuilding.getPosition(), target);
            if (isValidPosition(detourPos)) {
                System.out.println("[DEBUG] 靠近建筑" + blockingBuilding.getTilePosition() + " 建筑类型=" + blockingBuilding.getType() + ", position=" + target.toTilePosition() + "，绕行至: " + detourPos.toTilePosition());
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

    /**
     * 查找附近是否有气矿阻挡（只在近距离检测）
     */
    private static Unit findNearbyBlockingGeyser(Unit unit, Position target) {
        Position unitPos = unit.getPosition();

        return Games.game.getAllUnits().stream()
                .filter(u -> u.getType() == UnitType.Resource_Vespene_Geyser)
                .filter(u -> {
                    // 计算单位到气矿的距离
                    double distanceToGeyser = unitPos.getDistance(u.getPosition());

                    // 只有在很近距离（64像素，约2个Tile）才检测
                    // 这个距离应该是单位的碰撞体积 + 安全余量
                    if (distanceToGeyser > 64) {
                        return false;
                    }

                    // 确认气矿确实在前进方向上（避免绕后）
                    double distanceToTarget = unitPos.getDistance(target);
                    double geyserToTarget = u.getPosition().getDistance(target);

                    // 如果经过气矿更接近目标，才需要绕行
                    return geyserToTarget < distanceToTarget;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找附近是否有建筑阻挡（只在近距离检测）
     */
    private static Unit findNearbyBlockingBuilding(Unit unit, Position target) {
        Position unitPos = unit.getPosition();

        return Games.game.getAllUnits().stream()
                .filter(u -> u.getType().isBuilding())
                .filter(u -> u.isCompleted() || u.isBeingConstructed())
                .filter(u -> {
                    // 计算单位到建筑的距离
                    double distanceToBuilding = unitPos.getDistance(u.getPosition());

                    // 只有在近距离（48像素，约1.5个Tile）才检测
                    if (distanceToBuilding > 48) {
                        return false;
                    }

                    // 确认建筑在前进方向上
                    double distanceToTarget = unitPos.getDistance(target);
                    double buildingToTarget = u.getPosition().getDistance(target);

                    return buildingToTarget < distanceToTarget;
                })
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
     * @deprecated 不再使用远距离路径检测，改用近距离碰撞检测
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

    private static boolean checkStuck(Unit unit) {
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

        // 策略1：尝试横向和纵向绕行
        Position leftDetour = calculateHorizontalDetour(obstaclePos, -DETOUR_DISTANCE);
        Position rightDetour = calculateHorizontalDetour(obstaclePos, DETOUR_DISTANCE);
        Position upDetour = calculateVerticalDetour(obstaclePos, -DETOUR_DISTANCE);
        Position downDetour = calculateVerticalDetour(obstaclePos, DETOUR_DISTANCE);

        int leftScore = leftDetour != null ? evaluateClearance(leftDetour) : -1;
        int rightScore = rightDetour != null ? evaluateClearance(rightDetour) : -1;
        int upScore = upDetour != null ? evaluateClearance(upDetour) : -1;
        int downScore = downDetour != null ? evaluateClearance(downDetour) : -1;

        int maxScore = Math.max(Math.max(leftScore, rightScore), Math.max(upScore, downScore));

        if (maxScore > 0) {
            // 在得分相同的情况下，优先选择朝向目标的方向
            if (leftScore == maxScore && rightScore == maxScore) {
                return toTargetX >= 0 ? rightDetour : leftDetour;
            }
            if (upScore == maxScore && downScore == maxScore) {
                return toTargetY >= 0 ? downDetour : upDetour;
            }

            if (leftScore == maxScore) return leftDetour;
            if (rightScore == maxScore) return rightDetour;
            if (upScore == maxScore) return upDetour;
            if (downScore == maxScore) return downDetour;
        }

        // 策略2：如果直接绕行失败，尝试后退+横向偏移（V字形绕行）
        Position vShapeDetour = calculateVShapeDetour(currentPos, obstaclePos, target);
        if (isValidPosition(vShapeDetour)) {
            return vShapeDetour;
        }

        return null;
    }

    /**
     * 智能计算绕行位置，支持多次尝试不同距离
     */
    private static Position calculateSmartDetourPosition(Unit unit, Position obstaclePos, Position target) {
        int unitId = unit.getID();

        // 初始化尝试记录
        if (!attemptedDetours.containsKey(unitId)) {
            attemptedDetours.put(unitId, new java.util.HashSet<>());
        }

        java.util.Set<Position> attempted = attemptedDetours.get(unitId);

        // 尝试多个绕行距离（从小到大）
        int[] detourDistances = {96, 128, 160, 192};

        for (int distance : detourDistances) {
            // 计算四个方向的绕行点
            java.util.List<Position> candidates = new java.util.ArrayList<>();
            candidates.add(calculateHorizontalDetour(obstaclePos, -distance));
            candidates.add(calculateHorizontalDetour(obstaclePos, distance));
            candidates.add(calculateVerticalDetour(obstaclePos, -distance));
            candidates.add(calculateVerticalDetour(obstaclePos, distance));

            // 评估并排序候选点
            for (Position candidate : candidates) {
                if (isValidPosition(candidate) && !attempted.contains(candidate)) {
                    int clearance = evaluateClearance(candidate);
                    if (clearance > 0) {
                        // 检查候选点是否更接近目标
                        double distToTarget = candidate.getDistance(target);
                        if (distToTarget < obstaclePos.getDistance(target) + distance) {
                            attempted.add(candidate);
                            return candidate;
                        }
                    }
                }
            }
        }

        // 如果所有标准绕行都失败，尝试V字形绕行
        Position vShapeDetour = calculateVShapeDetour(unit.getPosition(), obstaclePos, target);
        if (isValidPosition(vShapeDetour) && !attempted.contains(vShapeDetour)) {
            attempted.add(vShapeDetour);
            return vShapeDetour;
        }

        // 清除历史记录，允许重新尝试（防止永久无法绕行）
        if (attempted.size() > 20) {
            attempted.clear();
            System.out.println("[DEBUG] 绕行尝试次数过多，清除历史记录");
        }

        return null;
    }


    /**
     * V字形绕行：后退 + 横向偏移
     */
    private static Position calculateVShapeDetour(Position currentPos, Position obstaclePos, Position target) {
        // 计算从障碍物到当前点的方向（后退方向）
        double backDx = currentPos.getX() - obstaclePos.getX();
        double backDy = currentPos.getY() - obstaclePos.getY();

        // 归一化
        double distance = Math.sqrt(backDx * backDx + backDy * backDy);
        if (distance == 0) {
            return null;
        }

        backDx /= distance;
        backDy /= distance;

        // 计算垂直于后退方向的横向向量
        double lateralDx = -backDy;  // 旋转90度
        double lateralDy = backDx;

        // 后退距离（32像素）+ 横向偏移（48像素）
        int backDistance = 32;
        int lateralDistance = 48;

        int newX = currentPos.getX() + (int) (backDx * backDistance + lateralDx * lateralDistance);
        int newY = currentPos.getY() + (int) (backDy * backDistance + lateralDy * lateralDistance);

        int mapWidth = Games.game.mapWidth() * 32;
        int mapHeight = Games.game.mapHeight() * 32;

        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));

        Position pos = new Position(newX, newY);
        return isValidPosition(pos) ? pos : null;
    }

    /**
     * 评估某个位置的通畅程度（周围可通行的格子数）
     */
    private static int evaluateClearance(Position pos) {
        int clearance = 0;
        int checkRadius = 64; // 检查范围（像素）
        int step = 32; // 检查步长（1个Tile）

        for (int dx = -checkRadius; dx <= checkRadius; dx += step) {
            for (int dy = -checkRadius; dy <= checkRadius; dy += step) {
                Position checkPos = new Position(pos.getX() + dx, pos.getY() + dy);
                if (isValidPosition(checkPos)) {
                    clearance++;
                }
            }
        }
        return clearance;
    }

    /**
     * 计算横向绕行点（左右绕行）
     */
    private static Position calculateHorizontalDetour(Position obstaclePos, int detourX) {
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
    private static Position calculateVerticalDetour(Position obstaclePos, int detourY) {
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
        attemptedDetours.remove(unitId);

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
            attemptedDetours.remove(unit.getID());
        }
    }

    public static void clearWorkerTracking() {
        lastPositions.clear();
        stuckFrames.clear();
        attemptedDetours.clear();
    }
}
