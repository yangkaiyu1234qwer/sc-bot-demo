package com.yangky.scbotdemo.bwem;

import bwapi.Unit;
import bwapi.UnitType;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 记录人族所有单位集合
 *
 * @author yangky
 * @Date 2026/4/29 14:19
 */
public class Units {
    private static final Map<UnitType, CopyOnWriteArraySet<Unit>> selfUnitMap = new HashMap<>();
    private static final Map<Integer, CopyOnWriteArraySet<Unit>> teamUnitMap = new HashMap<>();
    private static final Set<Unit> unitsBeBuilding = new HashSet<>();

    public static CopyOnWriteArraySet<Unit> getTeamUnits(int team) {
        return teamUnitMap.getOrDefault(team, new CopyOnWriteArraySet<>());
    }

    public static void unitCreated(Unit unit) {
        if (unit == null || unit.getPlayer() == null || unit.getPlayer() != Games.game.self()) {
            return;
        }
        CopyOnWriteArraySet<Unit> set = selfUnitMap.getOrDefault(unit.getType(), new CopyOnWriteArraySet<Unit>());
        if (set.add(unit)) {
            Supplies.maxSupplyUsedIncrement(unit.getType().supplyRequired());
        }
        selfUnitMap.put(unit.getType(), set);

        // 如果是正造建造的单位，则加入正在建造的集合
        if (unit.isBeingConstructed()) {
            unitsBeBuilding.add(unit);
        }

        // 探路农民编队1

        // 第一个基地遍队2
        if (unit.getType() == UnitType.Terran_Command_Center || unit.getType() == UnitType.Protoss_Nexus || unit.getType() == UnitType.Zerg_Lair) {// 是基地
            CopyOnWriteArraySet<Unit> team2 = teamUnitMap.getOrDefault(2, new CopyOnWriteArraySet<Unit>());
            // 没有第2队
            if (team2.isEmpty()) {
                team2.add(unit);
            }
            teamUnitMap.put(2, team2);
        }
        // 第一个兵营编队3
        if (unit.getType() == UnitType.Terran_Barracks || unit.getType() == UnitType.Protoss_Gateway || unit.getType() == UnitType.Zerg_Hatchery) {
            CopyOnWriteArraySet<Unit> team3 = teamUnitMap.getOrDefault(3, new CopyOnWriteArraySet<Unit>());
            team3.add(unit);
            teamUnitMap.put(3, team3);
        }
    }

    public static Set<Unit> getSelfUnits(UnitType type) {
        return selfUnitMap.getOrDefault(type, new CopyOnWriteArraySet<>());
    }

    public static void destroyUnit(Unit unit) {
        selfUnitMap.getOrDefault(unit.getType(), new CopyOnWriteArraySet<>()).remove(unit);
    }

    /**
     * 清理所有单位记录
     */
    public static void clearAll() {
        selfUnitMap.clear();
        teamUnitMap.clear();
        unitsBeBuilding.clear();
    }
}
