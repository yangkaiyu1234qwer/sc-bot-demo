package com.yangky.scbotdemo.listner.created;

import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.Workers;
import com.yangky.scbotdemo.util.Times;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Component
public class WorkerCreatedListener extends UnitCreatedListener {
    @Override
    public boolean apply(Unit unit) {
        return unit.getType().isWorker();
    }

    @Override
    public void onUnitTrained(Unit worker) {
        // 启动游戏时已经分配过农民
        if (Times.secondsFromStart() < 10) {
            return;
        }
        Set<Unit> centers = Units.getSelfUnits(UnitType.Terran_Command_Center);
        Set<Unit> nexus = Units.getSelfUnits(UnitType.Protoss_Nexus);
        Set<Unit> Hatcheries = Units.getSelfUnits(UnitType.Zerg_Hatchery);
        TreeSet<Unit> all = new TreeSet<>(Comparator.comparingInt(e -> e.getDistance(worker)));
        all.addAll(centers);
        all.addAll(nexus);
        all.addAll(Hatcheries);
        if (all.isEmpty()) {
            return;
        }
        Workers.goGatherLessLoader(worker, Bases.getNearestBase(worker.getTilePosition()));
    }

}
