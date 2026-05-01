package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Workers;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnFrameForGas extends OnFrame {
    @Override
    public Integer getInterval() {
        return 200;
    }

    @Override
    public void onFrame(Integer frame) {
        if (frame < 1000) {
            return;
        }
        Player self = Games.game.self();
        if (self.isNeutral() || self.isObserver() || self.isDefeated() || self.supplyUsed() < 36) {
            return;
        }
        List<Unit> bases = Bases.getBaseCenterList(self);
        bases.forEach(e -> {
            Base base = Bases.getBaseFormBaseUnit(e);
            base.getGeysers().forEach(geyser -> {
                // geyser 是 BWEM 的 Geyser 对象，包含位置和原始气矿 Unit
                Unit geyserUnit = geyser.getUnit(); // 原始气矿（Vespene Geyser）
                TilePosition geyserPos = geyserUnit.getTilePosition(); // 气矿位置
                // 1. 检查这个气矿上是否已建精炼厂
                Unit refinery = findRefineryAtPosition(geyserPos);
                if (refinery != null && refinery.isCompleted()) {
                    // 精炼厂已完成 → 分配工人采集
                    Unit worker = null;
                    for (long i = refinery.getLoadedUnits().size(); i < 3 && (worker = Workers.getAWorker(self, geyser.getCenter())) != null; i++) {
                        worker.gather(refinery);
                    }
                } else if (refinery == null) {
                    Builds.add(new BuildTask("gas-" + geyserPos.getX() + "-" + geyserPos.getY(),
                            geyserPos, UnitType.Terran_Refinery));
                }
            });
        });
    }

    private long countWorkersOnRefinery(Unit refinery) {
        return Games.game.self().getUnits().stream()
                .filter(u -> u.getType() == UnitType.Terran_SCV)
                .filter(u -> {
                    bwapi.Order order = u.getOrder();
                    if (order == null) {
                        return false;
                    }
                    // 检查是否在采集这个精炼厂
                    return (order == bwapi.Order.MoveToGas
                            || order == bwapi.Order.ReturnGas
                            || order == bwapi.Order.ResetCollision
                            || order == bwapi.Order.PlayerGuard)
                            && u.getTarget() != null
                            && u.getTarget().getID() == refinery.getID();
                })
                .count();
    }

    private Unit findRefineryAtPosition(TilePosition geyserPos) {
        return Games.game.self().getUnits().stream()
                .filter(u -> u.getType() == UnitType.Terran_Refinery)
                .filter(u -> {
                    TilePosition refPos = u.getTilePosition();
                    return Math.abs(refPos.getX() - geyserPos.getX()) <= 1 &&
                            Math.abs(refPos.getY() - geyserPos.getY()) <= 1;
                })
                .findFirst()
                .orElse(null);
    }

}
