package com.yangky.scbotdemo.bwem.walloff;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import com.yangky.scbotdemo.util.Positions;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class WallOffExecutor {
    private static final Set<String> completedBuildings = new HashSet<>();
    private static final Set<String> requiredBuildings = new HashSet<>();
    private static WallOff wallOff = null;

    private static boolean rescueExecuted = false;
    private static Unit rescueBarracks = null;
    private static TilePosition barracksLandPosition = null;
    private static int rescueWaitFrames = 0;

    static void markBuildingCompleted(String buildingId) {
        completedBuildings.add(buildingId);
        System.out.println("[堵口进度] " + buildingId + " 已完成 (" + completedBuildings.size() + "/" + requiredBuildings.size() + ")");
        if (isWallOffFinished()) {
            System.out.println("[堵口完成] ✓ 所有堵口建筑已建造完成！");
        }
        if (isBarracks(buildingId) && !rescueExecuted) {
            long unbuiltBarracksCount = requiredBuildings.stream()
                    .filter(WallOffExecutor::isBarracks)
                    .filter(id -> !completedBuildings.contains(id))
                    .count();
            if (unbuiltBarracksCount == 0) {
                triggerRescue(buildingId);
            }
        }
    }

    private static boolean isBarracks(String buildingId) {
        return buildingId != null && buildingId.toLowerCase().contains("barrack");
    }

    private static void triggerRescue(String barracksId) {
        Unit barracks = findBarracksByIdempotent(barracksId);
        if (barracks != null && barracks.exists() && barracks.isCompleted()) {
            System.out.println("[救援] 最后一个兵营完成，起飞让路");
            rescueBarracks = barracks;
            barracksLandPosition = barracks.getTilePosition();
            rescueWaitFrames = 0;

            if (barracks.lift()) {
                System.out.println("[救援] 兵营已起飞");
            }
        }
    }

    private static Unit findBarracksByIdempotent(String idempotentNo) {
        if (wallOff == null) {
            return null;
        }
        WallOffBuilding config = wallOff.getList().stream()
                .filter(wob -> wob.getIdempotentNo().equals(idempotentNo))
                .findFirst()
                .orElse(null);

        if (config == null || config.getTilePosition() == null) {
            return null;
        }

        TilePosition tilePos = config.getTilePosition();

        return Games.game.self().getUnits().stream()
                .filter(u -> u.getType() == UnitType.Terran_Barracks)
                .filter(u -> {
                    TilePosition uPos = u.getTilePosition();
                    return Math.abs(uPos.getX() - tilePos.getX()) <= 1 &&
                            Math.abs(uPos.getY() - tilePos.getY()) <= 1;
                })
                .findFirst()
                .orElse(null);
    }

    public static boolean isWallOffFinished() {
        if (requiredBuildings.isEmpty()) {
            return false;
        }
        return completedBuildings.containsAll(requiredBuildings);
    }

    public static void reset() {
        completedBuildings.clear();
        requiredBuildings.clear();
        wallOff = null;
        rescueExecuted = false;
        rescueBarracks = null;
        barracksLandPosition = null;
        rescueWaitFrames = 0;
    }

    private static void updateRescue() {
        if (rescueBarracks == null || rescueExecuted) {
            return;
        }
        rescueWaitFrames++;
        if (rescueWaitFrames >= 10) {//frame本身已经有帧数限制
            if (rescueBarracks.exists() && rescueBarracks.isFlying() && barracksLandPosition != null) {
                if (!rescueBarracks.isIdle()) rescueBarracks.cancelTrain();
                if (rescueBarracks.land(barracksLandPosition)) {
                    System.out.println("[救援] 兵营降落至: " + barracksLandPosition);
                }
            }
            System.out.println("[救援] 救援完成");
            rescueExecuted = true;
        }
    }

    public static void execute(Unit base) {
        if (wallOff == null) {
            if (WallOffConfig.getMap() == null || WallOffConfig.getMap().isEmpty()) {
                System.out.println("没有找到堵口坐标");
                return;
            }
            wallOff = WallOffConfig
                    .getMap()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(o -> base.getDistance(o.getKey().toPosition())))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            wallOff.getList().forEach(e -> {
                requiredBuildings.add(e.getIdempotentNo());
            });
            System.out.println("[堵口开始] 需要完成的建筑: " + requiredBuildings);
        }
        updateRescue();
        if (!isWallOffFinished()) {
            wallOff.getList().forEach(e -> {
                if (e.getTilePosition() == null) {
                    e.setTilePosition(Positions.getEdgePosition(e.getUnitType(), base, 0));
                }
                if (completedBuildings.contains(e.getIdempotentNo())) {
                    return;
                }
                if (!Builds.idempotent(e.getIdempotentNo()) && e.getBuildTiming().accept(Games.game)) {
                    System.out.println("建造堵口建筑: " + e.getIdempotentNo() + " - " + e.getUnitType() + " at " + e.getTilePosition());
                    BuildTask task = new BuildTask(e.getIdempotentNo(), e.getTilePosition(), e.getUnitType(), () -> {
                        if (e.getCallback() != null) {
                            e.getCallback().execute();
                        }
                        markBuildingCompleted(e.getIdempotentNo());
                    });
                    Builds.add(task);
                }
            });
        }
        CopyOnWriteArraySet<Unit> barracks = Units.getTeamUnits(3);
        if (!barracks.isEmpty()) {
            Unit firstBarracks = barracks.iterator().next();
            if (firstBarracks.isIdle() && Units.getSelfUnits(UnitType.Terran_Marine).size() < 4) {
                firstBarracks.train(UnitType.Terran_Marine);
            }
        }
        Unit bunker = findNearestBunkerWithSpace();
        if (bunker == null) {
            return;
        }
        // 同时检查闲置枪兵，命令进地堡
        Unit idleMarine = Units.getSelfUnits(UnitType.Terran_Marine).stream()
                .filter(Unit::isIdle)
                .findFirst()
                .orElse(null);
        if (idleMarine != null) {
            idleMarine.rightClick(bunker);
            System.out.println("[训练] 枪兵进地堡，当前地堡容量: " + bunker.getLoadedUnits().size());
        }
    }

    private static Unit findNearestBunkerWithSpace() {
        Set<Unit> bunkers = Units.getSelfUnits(UnitType.Terran_Bunker);
        return bunkers.stream()
                .filter(u -> u.getType() == UnitType.Terran_Bunker)
                .filter(Unit::isCompleted)
                .filter(u -> u.getLoadedUnits().size() < 4)
                .findFirst()
                .orElse(null);
    }

    public static Set<String> getWallOffPositions() {
        if (wallOff == null) {
            return new HashSet<>();
        }
        return wallOff.getList().stream()
                .map(WallOffBuilding::getTilePosition)
                .filter(Objects::nonNull)
                .map(pos -> pos.getX() + "," + pos.getY())
                .collect(Collectors.toSet());
    }

}
