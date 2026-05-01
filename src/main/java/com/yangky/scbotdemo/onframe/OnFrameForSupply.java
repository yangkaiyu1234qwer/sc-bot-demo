package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Supplies;
import com.yangky.scbotdemo.bwem.task.BuildTask;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;
import com.yangky.scbotdemo.util.Positions;
import com.yangky.scbotdemo.util.Properties;
import com.yangky.scbotdemo.util.Times;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * SupplyOnframe
 *
 * @author yangky
 * @Date 2026/4/28 4:14
 */
@Component
@AllArgsConstructor
public class OnFrameForSupply extends OnFrame {
    private Properties properties;

    @Override
    public Integer getInterval() {
        return Times.SUPPLY_INTERVAL;
    }

    @Override
    public void onFrame(Integer frame) {
        // 堵口开启且堵口未完成
        if (properties.isWallOffOn() && !WallOffExecutor.isWallOffFinished()) {
            return;
        }
        Player player = Games.game.self();
        int supplyDeficit = Supplies.supplyDeficit(player);
        if (supplyDeficit > 0) {
            // 补人口
            UnitType supplyBuilding = Supplies.getSupplyUnitType(player);
            List<Unit> baseList = Bases.getBaseCenterList(player);
            if (CollectionUtils.isEmpty(baseList)) return;
            Unit mainBase = baseList.get(0);
            TilePosition buildPos = Positions.getEdgePosition(supplyBuilding, mainBase);
            if (buildPos == null) {
                return;
            }
            String idempotentNo = "supply_from_" + player.supplyTotal() + "_to_" + (player.supplyTotal() + 8 * supplyDeficit);
            // 创建待处理任务
            Builds.add(new BuildTask(idempotentNo, buildPos, supplyBuilding));
        }
    }

}
