package com.yangky.scbotdemo.onframe;

import bwapi.*;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.region.RegionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OnFrameTank
 *
 * @author yangky
 * @Date 2026/5/1 13:51
 */
@Component
public class OnFrameForTank extends OnFrame {
    @Override
    public Integer getInterval() {
        return 25;
    }

    @Override
    public void onFrame(Integer frame) {
        Player self = Games.game.self();
        Unit base = Bases.getMainBaseUnit();
        // 造VF
        long completedFactories = Units.getSelfUnits(UnitType.Terran_Factory).stream().filter(Unit::isCompleted).count();
        long buildingFactories = BuildExecutor.getCountByBuildingType(UnitType.Terran_Factory);
        long factoryCount = completedFactories + buildingFactories;
        if (self.minerals() >= 100 && self.gas() >= 50 && self.supplyUsed() >= 40) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.CENTRAL);
            TilePosition pos = BuildingPlacer.findPosition(UnitType.Terran_Factory, regionTypes, 3, 0, true);
            if (factoryCount < 1) {
                BuildExecutor.add(Task.of("factory_" + 1, pos, UnitType.Terran_Factory));
            } else if (factoryCount < 2) {
                BuildExecutor.add(Task.of("factory_" + 2, pos, UnitType.Terran_Factory));
            }
        }
        // 造BE
        long barracksCount = Units.getSelfUnits(UnitType.Terran_Barracks).stream().filter(Unit::isCompleted).count();
        long engineerCount = BuildExecutor.getCountByBuildingType(UnitType.Terran_Engineering_Bay) +
                Units.getSelfUnits(UnitType.Terran_Engineering_Bay).stream().filter(Unit::isCompleted).count();
        if (engineerCount < 1 && barracksCount > 0 && factoryCount > 0 && self.supplyUsed() >= 60) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.CENTRAL);
            TilePosition pos = BuildingPlacer.findPosition(UnitType.Terran_Engineering_Bay, regionTypes, 0, 0, true);
//            Builds.add(new BuildTask("engineer_" + (++engineerCount), pos, UnitType.Terran_Engineering_Bay, null));
            BuildExecutor.add(Task.of("engineerCount", pos, UnitType.Terran_Engineering_Bay));
        } else {
            Units.getSelfUnits(UnitType.Terran_Engineering_Bay).forEach(Unit::lift);
        }

        // 放附件 - 使用重试机制
        Set<Unit> factories = Units.getSelfUnits(UnitType.Terran_Factory);
        factories.stream()
                .filter(e -> e.isCompleted() && e.getAddon() == null)
                .forEach(e -> {
                    if (e.canBuildAddon()) {
                        e.buildAddon(UnitType.Terran_Machine_Shop);
                    }
                    // 设置集结点
                    Set<Unit> bunkers = Units.getSelfUnits(UnitType.Terran_Bunker);
                    Position rallyPoint = bunkers.isEmpty() ? base.getPosition() : bunkers.iterator().next().getPosition();
                    e.setRallyPoint(rallyPoint);
                });
        // 升级支架
        if (!self.hasResearched(TechType.Tank_Siege_Mode)) {
            Set<Unit> armories = Units.getSelfUnits(UnitType.Terran_Machine_Shop);
            armories.stream().filter(e -> e.isCompleted() && e.isIdle()).findFirst().ifPresent(armory -> armory.research(TechType.Tank_Siege_Mode));
        }
        // 造坦克
        // 设置集结点
        Unit barrack = self.getUnits().stream()
                .filter(e -> e.getType() == UnitType.Terran_Barracks && e.isCompleted())
                .max(Comparator.comparing(e -> e.getDistance(base.getPosition())))
                .orElse(null);
        factories.stream().filter(e -> e.isCompleted() && e.getAddon() != null && e.getAddon().isCompleted() && e.isIdle()).forEach(e -> {
            Set<Unit> tanks = Units.getSelfUnits(UnitType.Terran_Siege_Tank_Tank_Mode);
            Set<Unit> siegeTanks = Units.getSelfUnits(UnitType.Terran_Siege_Tank_Siege_Mode);
            if (tanks.size() + siegeTanks.size() >= 14) {
                return;
            }
            e.train(UnitType.Terran_Siege_Tank_Tank_Mode);
            if (barrack != null) {
                e.setRallyPoint(barrack.getTilePosition().toPosition());
            }
            siegeTanks.stream().max(Comparator.comparing(t -> t.getDistance(base.getPosition()))).ifPresent(e::setRallyPoint);
        });
        if (Supplies.getMaxSupplyUsed() < 80 || Units.getSelfUnits(UnitType.Terran_Engineering_Bay).isEmpty()) {
            return;
        }
        // 路口坦克支架
//        Set<Unit> tanks = self.getUnits().stream()
//                .filter(e -> e.getType() == UnitType.Terran_Siege_Tank_Tank_Mode || e.getType() == UnitType.Terran_Siege_Tank_Siege_Mode)
//                .collect(Collectors.toSet());
//        for (Unit tank : tanks) {
//            if (!tank.isSieged() && self.hasResearched(TechType.Tank_Siege_Mode)) {
//                if (barrack != null) {
//                    if (tank.getDistance(barrack) < 5) {
//                        tank.siege();
//                    }
//                } else if (tank.isIdle()) {
//                    tank.siege();
//                }
//            }
//        }
        // 受到攻击的建筑拉scv修复
        Set<Unit> fixedUnits = Games.game.self().getUnits().stream()
                .filter(e -> e.getHitPoints() < e.getType().maxHitPoints() && e.getType().isMechanical() && e.getType().isBuilding())
                .collect(Collectors.toSet());
        for (Unit building : fixedUnits) {
            Set<Unit> repairWorkers = Workers.getRepairingWorkers(building);
            for (int i = repairWorkers.size(); i < 2; i++) {
                Unit worker = Workers.getRepairWorker(building);
                if (worker != null) {
                    worker.repair(building);
                    Workers.markRepairingWorker(worker, building);
                }
            }
        }
        // 边缘区域修防空
        Set<Unit> missiles = Units.getSelfUnits(UnitType.Terran_Missile_Turret);
        int missileBuildingCount = BuildExecutor.getCountByBuildingType(UnitType.Terran_Missile_Turret);
        if (missiles.size() < 1 && missileBuildingCount < 1) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.CHOKE_POINT);
            regionTypes.add(RegionType.BOUNDARY);
            regionTypes.add(RegionType.EDGE);
            TilePosition newPos = BuildingPlacer.findPosition(UnitType.Terran_Missile_Turret, regionTypes, 3, 3, true);
            if (newPos != null) {
                BuildExecutor.add(Task.of("missiles_turret_" + missiles.size() + 1, newPos, UnitType.Terran_Missile_Turret));
            }
        } else if (missiles.size() < 45 && missileBuildingCount < 4) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.BOUNDARY);
            regionTypes.add(RegionType.EDGE);
            regionTypes.add(RegionType.CHOKE_POINT);
            regionTypes.add(RegionType.MINERAL);
            TilePosition newPos = BuildingPlacer.findPosition(UnitType.Terran_Missile_Turret, regionTypes, 3, 3, false);
            if (newPos != null) {
                BuildExecutor.add(Task.of("missiles_turret_" + missiles.size() + 1, newPos, UnitType.Terran_Missile_Turret));
            }
        }

    }
}
