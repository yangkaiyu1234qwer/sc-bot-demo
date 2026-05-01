package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * OnFrameForGas
 *
 * @author yangky
 * @Date 2026/5/1
 */
@Component
public class OnFrameForGas extends OnFrame {
    @Override
    public Integer getInterval() {
        return 200;
    }

    @Override
    public void onFrame(Integer frame) {
        if (frame < 2000) {
            return;
        }
        Player self = Games.game.self();
        if (self.isNeutral() || self.isObserver() || self.isDefeated() || self.supplyUsed() < 30) {
            return;
        }
        List<Unit> bases = Bases.getBaseCenterList(self);
        bases.forEach(e -> {
            Base base = Bases.getBaseFormBaseUnit(e);
            base.getGeysers().forEach(geyser -> {
                Unit refinery = findRefineryForGeyser(geyser.getUnit());
                if (refinery != null && refinery.isCompleted()) {
                    long actualWorkerCount = Workers.getGasWorkerCount(refinery);
                    if (actualWorkerCount < 3) {
                        // ✅ 分配新工人，但要排除已经在采气的
                        Unit worker = getAvailableWorker(self, geyser.getCenter());
                        if (worker != null) {
                            Workers.goGatherGas(worker, refinery);
                            System.out.println("[气矿] 分配工人 " + worker.getID() + " 采集气矿，当前: " + (actualWorkerCount + 1) + "/3");
                        }
                    }
                } else {
                    Set<Unit> refineries = Units.getSelfUnits(UnitType.Terran_Refinery);
                    if (refineries.size() < 1 && self.supplyUsed() >= 12) {
                        Builds.add(new BuildTask("gas1", geyser.getUnit().getTilePosition(), UnitType.Terran_Refinery));
                    } else if (refineries.size() < 2 && self.supplyUsed() >= 35) {
                        Builds.add(new BuildTask("gas2", geyser.getUnit().getTilePosition(), UnitType.Terran_Refinery));
                    }
                }
            });
        });
    }

    /**
     * 获取可用的工人（排除已经在采气的）
     */
    private Unit getAvailableWorker(Player player, Position target) {
        return player.getUnits().stream()
                .filter(u -> u.getType().isWorker())
                .filter(u -> u.isIdle() || u.isGatheringMinerals())  // 只选闲置或采矿的
                .filter(u -> !Workers.isGasWorker(u))  // ✅ 排除已在采气的
                .filter(u -> !Builds.getBuildingWorkers().contains(u))  // 排除建造中的
                .min(Comparator.comparingInt(u -> u.getDistance(target)))
                .orElse(null);
    }

    private Unit findRefineryForGeyser(Unit geyser) {
        if (geyser == null) {
            return null;
        }
        return Games.game.self().getUnits().stream()
                .filter(u -> u.getType() == UnitType.Terran_Refinery)
                .filter(u -> {
                    int dx = Math.abs(u.getTilePosition().getX() - geyser.getTilePosition().getX());
                    int dy = Math.abs(u.getTilePosition().getY() - geyser.getTilePosition().getY());
                    return dx <= 1 && dy <= 1;
                })
                .findFirst()
                .orElse(null);
    }
}
