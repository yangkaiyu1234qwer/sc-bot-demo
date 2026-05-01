package com.yangky.scbotdemo.onframe;

import bwapi.*;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import com.yangky.scbotdemo.util.Positions;
import org.springframework.stereotype.Component;

import java.util.Set;

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
        long buildingFactories = Builds.getCountByBuildingType(UnitType.Terran_Factory) - completedFactories;
        long factoryCount = completedFactories + buildingFactories;
        if (self.minerals() >= 100 && self.gas() >= 50 && self.supplyUsed() >= 40) {
            if (factoryCount < 1) {
                TilePosition pos = Positions.getHeartlandPosition(UnitType.Terran_Factory, base);
                Builds.add(new BuildTask("factory_1", pos, UnitType.Terran_Factory, null));  // ✅ 固定 ID
            } else if (factoryCount < 2) {
                TilePosition pos = Positions.getHeartlandPosition(UnitType.Terran_Factory, base);
                Builds.add(new BuildTask("factory_2", pos, UnitType.Terran_Factory, null));  // ✅ 固定 ID
            }
        }
        // 造BE
        int barracksCount = Builds.getCountByBuildingType(UnitType.Terran_Barracks);
        int engineerCount = Builds.getCountByBuildingType(UnitType.Terran_Engineering_Bay);
        if (engineerCount < 1 && barracksCount > 0 && factoryCount > 0 && self.supplyUsed() >= 60) {
            TilePosition pos = Positions.getEdgePosition(UnitType.Terran_Engineering_Bay, base);
            Builds.add(new BuildTask("engineer_" + (++engineerCount), pos, UnitType.Terran_Engineering_Bay, null));
        }
        // 放附件
        Set<Unit> factories = Units.getSelfUnits(UnitType.Terran_Factory);
        factories.stream().filter(e -> e.isCompleted() && e.getAddon() == null && e.isIdle()).forEach(e -> {
            e.buildAddon(UnitType.Terran_Armory);
            // 设置集结点
            Set<Unit> bunkers = Units.getSelfUnits(UnitType.Terran_Bunker);
            Position rallyPoint = bunkers.isEmpty() ? base.getPosition() : bunkers.iterator().next().getPosition();
            e.setRallyPoint(rallyPoint);
        });
        // 升级支架
        if (!self.hasResearched(TechType.Tank_Siege_Mode)) {
            Set<Unit> armories = Units.getSelfUnits(UnitType.Terran_Armory);
            armories.stream().filter(e -> e.isCompleted() && e.isIdle()).findFirst().ifPresent(armory -> armory.research(TechType.Tank_Siege_Mode));
        }
        // 造坦克
        factories.stream().filter(e -> e.isCompleted() && e.getAddon() != null && e.getAddon().isCompleted() && e.isIdle()).forEach(e -> {
            e.train(UnitType.Terran_Siege_Tank_Tank_Mode);
            // 设置集结点
            e.setRallyPoint(Units.getSelfUnits(UnitType.Terran_Bunker).iterator().next());
        });
    }
}
