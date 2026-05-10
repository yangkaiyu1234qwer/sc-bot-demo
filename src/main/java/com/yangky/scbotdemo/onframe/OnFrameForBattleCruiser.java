package com.yangky.scbotdemo.onframe;


import bwapi.*;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.region.RegionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * OnFrameForBattleCruiser - 大和战舰生产逻辑
 *
 * @author yangky
 * @Date 2026/5/2
 */
@Component
public class OnFrameForBattleCruiser extends OnFrame {
    @Override
    public Integer getInterval() {
        return 25;
    }

    @Override
    public void onFrame(Integer frame) {
        Player self = Games.game.self();
        Unit base = Bases.getMainBaseUnit();

        // 触发条件：人口 >= 100 (API中是双倍), 矿 >= 500, 气 >= 300
        if (self.supplyUsed() < 100 || self.minerals() < 500 || self.gas() < 300) {
            return;
        }

        // ==================== 1. 造机场 (Starport) ====================
        long completedStarports = Units.getSelfUnits(UnitType.Terran_Starport).stream()
                .filter(Unit::isCompleted)
                .count();
        long buildingStarports = BuildExecutor.getCountByBuildingType(UnitType.Terran_Starport);
        long starportCount = completedStarports + buildingStarports;

        if (starportCount < 5) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.CENTRAL);
            TilePosition pos = BuildingPlacer.findPosition(UnitType.Terran_Starport, regionTypes, 0, 3, true);
            if (pos != null) {
                String taskId = "starport_" + (starportCount + 1);
                BuildExecutor.add(Task.of(taskId, pos, UnitType.Terran_Starport));
            }
        }

        // ==================== 2. 挂附件 (Control Tower) ====================
        Set<Unit> starports = Units.getSelfUnits(UnitType.Terran_Starport);
        starports.stream()
                .filter(e -> e.isCompleted() && e.getAddon() == null)
                .forEach(e -> {
                    if (e.canBuildAddon()) {
                        e.buildAddon(UnitType.Terran_Control_Tower);
                    }
                });

        // 造VI Terran_Science_Facility
        long completedFacilities = Units.getSelfUnits(UnitType.Terran_Science_Facility).stream()
                .filter(Unit::isCompleted)
                .count();
        long buildingFacilities = BuildExecutor.getCountByBuildingType(UnitType.Terran_Science_Facility);
        long facilityCount = completedFacilities + buildingFacilities;
        if (facilityCount < 1) {
            List<RegionType> regionTypes = new ArrayList<>();
            regionTypes.add(RegionType.CENTRAL);
            TilePosition pos = BuildingPlacer.findPosition(UnitType.Terran_Science_Facility, regionTypes, 1, 1, true);
//            TilePosition pos = Positions.getCentralPosition(UnitType.Terran_Science_Facility, Locations.getCentralAreaCenter().toPosition().toTilePosition());
//            Builds.add(new BuildTask("facility_1", pos, UnitType.Terran_Science_Facility, null));
            BuildExecutor.add(Task.of("facility_1", pos, UnitType.Terran_Science_Facility));
        }

        // ==================== 3. 造科学研究院 (Physics Lab) ====================
        // Physics Lab 是挂在 Starport 上的附件，需要先有 Control Tower
        long completedPhysicsLabs = Units.getSelfUnits(UnitType.Terran_Physics_Lab).stream()
                .filter(Unit::isCompleted)
                .count();
        long buildingPhysicsLabs = BuildExecutor.getCountByBuildingType(UnitType.Terran_Physics_Lab);
        long physicsLabCount = completedPhysicsLabs + buildingPhysicsLabs;
        // 至少有一个挂了 Control Tower 的 Starport 才能造 Physics Lab
        if (physicsLabCount < 1) {
            Unit facility = self.getUnits().stream()
                    .filter(e -> e.isCompleted() && e.getType() == UnitType.Terran_Science_Facility)
                    .findFirst()
                    .orElse(null);
            if (facility != null && facility.canBuildAddon()) {
                // Physics Lab 需要替换 Control Tower，所以先移除 Control Tower
                // BWAPI 会自动处理附件替换逻辑
                facility.buildAddon(UnitType.Terran_Physics_Lab);
            }
        }

        // ==================== 4. 升级大和炮 (Yamato Cannon) ====================
        if (!self.hasResearched(TechType.Yamato_Gun)) {
            Set<Unit> physicsLabs = Units.getSelfUnits(UnitType.Terran_Physics_Lab);
            physicsLabs.stream()
                    .filter(e -> e.isCompleted() && e.isIdle())
                    .findFirst()
                    .ifPresent(lab -> lab.research(TechType.Yamato_Gun));
        }
        // ==================== 5. 生产大和战舰 (BattleCruiser) ====================
        // 目标数量：2队 = 24个 (BWAPI中单位计数)
        int targetCount = 24;
        long completedBCs = self.getUnits().stream()
                .filter(e -> e.getType() == UnitType.Terran_Battlecruiser)
                .filter(Unit::isCompleted)
                .count();
        if (completedBCs < targetCount) {
            for (Unit starport : starports) {
                if (completedBCs >= targetCount) {
                    break;
                }
                if (starport.canTrain(UnitType.Terran_Battlecruiser) && starport.isIdle()) {
                    starport.train(UnitType.Terran_Battlecruiser);
                    completedBCs++;
                    // 设置集结点（可选：指向基地或前线）
                    if (base != null) {
                        starport.setRallyPoint(base.getPosition());
                    }
                }
            }
        }
    }
}