package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
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

    // ✅ 记录已经分配去采气的 SCV
    private static final Set<Unit> gasWorkers = new HashSet<>();

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
        if (self.isNeutral() || self.isObserver() || self.isDefeated() || self.supplyUsed() < 28) {
            return;
        }
        // ✅ 清理已死亡或不再采气的 SCV
        cleanInvalidGasWorkers();
        List<Unit> bases = Bases.getBaseCenterList(self);
        bases.forEach(e -> {
            Base base = Bases.getBaseFormBaseUnit(e);
            base.getGeysers().forEach(geyser -> {
                Unit refinery = findRefineryForGeyser(geyser.getUnit());
                if (refinery != null && refinery.isCompleted()) {
                    // ✅ 统计真正在采气的 SCV（排除已记录但实际没在采的）
                    int actualWorkerCount = countActualGasWorkers(refinery);
                    if (actualWorkerCount < 3) {
                        // ✅ 分配新工人，但要排除已经在采气的
                        Unit worker = getAvailableWorker(self, geyser.getCenter());
                        if (worker != null) {
                            worker.gather(refinery);
                            gasWorkers.add(worker);  // ✅ 记录
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
     * 清理无效的采气工人记录
     */
    private void cleanInvalidGasWorkers() {
        gasWorkers.removeIf(worker -> {
            if (worker == null || !worker.exists()) {
                return true;  // 工人死亡，移除记录
            }
            // 检查工人是否还在采气
            bwapi.Order order = worker.getOrder();
            return order != bwapi.Order.MoveToGas &&
                    order != bwapi.Order.ReturnGas &&
                    order != bwapi.Order.ResetCollision;  // 不再采气，移除记录
        });
    }

    /**
     * 统计真正在采气的 SCV 数量
     */
    private int countActualGasWorkers(Unit refinery) {
        int count = 0;
        for (Unit worker : gasWorkers) {
            if (worker != null && worker.exists()) {
                Unit target = worker.getTarget();
                if (target != null && target.getID() == refinery.getID()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 获取可用的工人（排除已经在采气的）
     */
    private Unit getAvailableWorker(Player player, Position target) {
        return player.getUnits().stream()
                .filter(u -> u.getType().isWorker())
                .filter(u -> u.isIdle() || u.isGatheringMinerals())  // 只选闲置或采矿的
                .filter(u -> !gasWorkers.contains(u))  // ✅ 排除已在采气的
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
