package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.util.Positions;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * OnFrameForAirUpgrade - 空军攻防升级逻辑
 *
 * @author yangky
 * @Date 2026/5/2
 */
@Component
public class OnFrameForAirUpgrade extends OnFrame {
    @Override
    public Integer getInterval() {
        return 25;
    }

    @Override
    public void onFrame(Integer frame) {
        Player self = Games.game.self();
        Unit base = Bases.getMainBaseUnit();
        if (base == null) return;
        // 先检查有没有机场
        Set<Unit> airPorts = self.getUnits().stream()
                .filter(e -> e.getType() == UnitType.Terran_Starport)
                .filter(Unit::isCompleted)
                .collect(Collectors.toSet());
        if (airPorts.isEmpty()) {
            return;
        }

        // ==================== 1. 造军械库 (Armory) ====================
        long completedArmories = Units.getSelfUnits(UnitType.Terran_Armory).stream()
                .filter(Unit::isCompleted)
                .count();
        long buildingArmories = BuildExecutor.getCountByBuildingType(UnitType.Terran_Armory);
        long armoryCount = completedArmories + buildingArmories;

        // 目标：造 2 个 Armory
        if (armoryCount < 2) {
            for (long i = armoryCount; i < 2; i++) {
                // 使用 EdgePosition，Armory 通常放在边缘或腹地
                bwapi.TilePosition pos = Positions.getEdgePosition(UnitType.Terran_Armory, base);
                if (pos != null) {
//                    Builds.add(new BuildTask("armory_" + (i + 1), pos, UnitType.Terran_Armory, null));
                    BuildExecutor.add(Task.of("armory_" + (i + 1), pos, UnitType.Terran_Armory));
                }
            }
        }

        // ==================== 2. 升级空军攻防 ====================
        // 只有当 Armory 建好后才能升级
        if (completedArmories > 0) {
            Set<Unit> armories = Units.getSelfUnits(UnitType.Terran_Armory);

            // 获取当前升级等级
            int airAttackLevel = self.getUpgradeLevel(UpgradeType.Terran_Ship_Weapons);
            int airDefenseLevel = self.getUpgradeLevel(UpgradeType.Terran_Ship_Plating);

            // 最大升级等级通常是 3
            int maxLevel = 3;

            // 升级空军攻击
            if (airAttackLevel < maxLevel) {
                armories.stream()
                        .filter(e -> e.isCompleted() && e.isIdle())
                        .findFirst()
                        .ifPresent(armory -> {
                            armory.upgrade(UpgradeType.Terran_Ship_Weapons);
                            System.out.println("[AirUpgrade] 开始升级空军攻击: Level " + (airAttackLevel + 1));
                        });
            }
            // 升级空军防御
            else if (airDefenseLevel < maxLevel) {
                armories.stream()
                        .filter(e -> e.isCompleted() && e.isIdle())
                        .findFirst()
                        .ifPresent(armory -> {
                            armory.upgrade(UpgradeType.Terran_Ship_Plating);
                            System.out.println("[AirUpgrade] 开始升级空军防御: Level " + (airDefenseLevel + 1));
                        });
            }
        }
    }
}
