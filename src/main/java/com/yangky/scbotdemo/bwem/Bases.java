package com.yangky.scbotdemo.bwem;

import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import bwem.Mineral;
import com.yangky.scbotdemo.util.Maths;

import java.util.*;
import java.util.stream.Collectors;

public class Bases {

    /**
     * 判断一个单位是否是基地建筑
     */
    public static boolean isBaseBuilding(UnitType type) {
        return type == UnitType.Protoss_Nexus || type == UnitType.Terran_Command_Center || type == UnitType.Zerg_Hatchery;
    }

    /**
     * 判断一片矿区是否由该玩家占领
     */
    public static Boolean isBaseOccupied(Player player, Base base) {
        TilePosition tilePosition = base.getLocation();
        Unit unit = Games.game.getUnitsOnTile(tilePosition).stream().findFirst().orElse(null);
        return Objects.nonNull(unit) && isBaseBuilding(unit.getType()) && Objects.equals(player, unit.getPlayer());
    }


    /**
     * 获取玩家的基地列表
     */
    public static List<Unit> getBaseCenterList(Player player) {
        if (player == null) {
            return Collections.emptyList();
        }
        List<Unit> units = player.getUnits();
        if (units == null || units.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return units.stream()
                    .sorted(Comparator.comparingInt(e -> e.getDistance(player.getStartLocation().toPosition())))
                    .filter(e -> isBaseBuilding(e.getType()))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            // 某些地图可能没有 StartLocation
            System.out.println("警告：无法获取起始位置，使用简单排序");
            return units.stream()
                    .filter(e -> isBaseBuilding(e.getType()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 【核心方法】
     * 根据基地建筑自身类型，返回它能生产的农民
     */
    public static UnitType getBaseProducedUnit(Unit unit) {
        if (unit == null || !unit.exists()) {
            return null;
        }
        UnitType type = unit.getType();
        // 人族基地 → 生产 SCV
        if (type == UnitType.Terran_Command_Center) {
            return UnitType.Terran_SCV;
        }
        // 神族基地 → 生产 Probe
        if (type == UnitType.Protoss_Nexus) {
            return UnitType.Protoss_Probe;
        }
        // 虫族基地 → 生产 Drone
        if (type == UnitType.Zerg_Hatchery || type == UnitType.Zerg_Lair || type == UnitType.Zerg_Hive) {
            return UnitType.Zerg_Drone;
        }
        return null; // 不是基地
    }

    /**
     * 指定基地补农民
     */
    private static void trainWorker(Unit base) {
        if (Supplies.isSupplyEnough(base.getType().supplyRequired())) {
            base.train(base.getType().getRace().getWorker());
        }
    }

    /**
     * 指定基地在空闲状态时才补农民
     */
    public static void trainWorkerIfIdle(Unit base) {
        List<Unit> list = new ArrayList<>();
        list.add(base);
        trainWorkerIfIdle(list);
    }

    public static void trainWorkerIfIdle(List<Unit> bases) {
        bases.forEach(e -> {
            if (e.getPlayer().minerals() < 50 || !e.isIdle() || !e.canTrain()) {
                return;
            }
            Base base = Bases.getBaseFormBaseUnit(e);
            if (Objects.isNull(base)) {
                return;
            }
            // 获取基地周围的水晶矿
            List<Mineral> minerals = base.getMinerals();
            // 获取基地周围正在采集水晶的农民
            List<Unit> workers = Workers.getWorkersByMineral(e.getPlayer(), e);
            if (workers.size() < Maths.mul(minerals.size(), 2)) {
                trainWorker(e);
            }
        });
    }

    public static Base getBaseFormBaseUnit(Unit baseUnit) {
        if (baseUnit == null || !baseUnit.exists() || baseUnit.isFlying()) {
            return null;
        }
        return Games.bwem.getMap()
                .getBases()
                .stream()
                .filter(e -> e.getLocation().equals(baseUnit.getTilePosition()))
                .findFirst().
                orElse(null);
    }

    public static Base getMainBase() {
        return Games.bwem.getMap()
                .getBases()
                .stream()
                .filter(e -> e.isStartingLocation())
                .findFirst().
                orElse(null);
    }

    public static Unit getMainBaseUnit() {
        Unit mainBase = getNearestBase(Games.game.self().getStartLocation());
        // 获取不到，从编队2获取
        if (mainBase == null) {
            Set<Unit> bases = Units.getTeamUnits(2);
            Unit mainBaseUnit = bases.stream()
                    .min(Comparator.comparingInt(e -> e.getDistance(Games.game.self().getStartLocation().toPosition())))
                    .orElse(null);
            if (mainBaseUnit != null) {
                return mainBaseUnit;
            }
        }
        return mainBase;
    }

    /**
     * 获取最接近指定位置的基地
     */
    public static Unit getNearestBase(TilePosition tilePosition) {
        Set<Unit> bases = Units.getTeamUnits(2);

        Base mainBase = Games.bwem.getMap()
                .getBases()
                .stream()
                .min(Comparator.comparingInt(e -> e.getLocation().getApproxDistance(tilePosition)))
                .orElse(null);
        if (mainBase == null) {
            mainBase = Games.bwem.getMap()
                    .getBases()
                    .stream()
                    .min(Comparator.comparingInt(e -> e.getLocation().getApproxDistance(tilePosition)))
                    .orElse(null);
        }
        return bases.stream()
                .min(Comparator.comparingInt(e -> tilePosition.getApproxDistance(e.getTilePosition())))
                .orElse(null);
    }
}
