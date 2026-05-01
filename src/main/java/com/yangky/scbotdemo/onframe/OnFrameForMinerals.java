package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.Unit;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Workers;
import com.yangky.scbotdemo.util.Times;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * EconomicOnFrame
 *
 * @author yangky
 * @Date 2026/4/28 4:18
 */
@Component
public class OnFrameForMinerals extends OnFrame {
    @Override
    public Integer getInterval() {
        return Times.ECONOMIC_INTERVAL;
    }

    @Override
    public void onFrame(Integer frame) {
        Player self = Games.game.self();
        // 补农民
        if (self.minerals() >= 50) {
            Bases.trainWorkerIfIdle(Bases.getBaseCenterList(self));
        }
        //前100帧（约4秒）不重新分配农民
        if (frame < 100) {
            return;
        }
        // 调试阶段 10分钟后不再调配闲置农民
        if (Times.secondsFromStart() > 600 * 1000) {
            return;
        }
        // 空闲农民采矿
        List<Unit> workersOnBuilding = Builds.getBuildingWorkers();
        List<Unit> idleWorkers = self.getUnits().stream()
                .filter(e -> e.getType().isWorker() && e.isIdle())
                .filter(e -> !Workers.isBuilder(e))
                .collect(Collectors.toList());
        idleWorkers = idleWorkers.stream().filter(e -> !workersOnBuilding.contains(e)).collect(Collectors.toList());
        idleWorkers.forEach(e -> {
            Workers.goGatherLessLoader(e, Bases.getNearestBase(e.getTilePosition()));
        });
    }
}
