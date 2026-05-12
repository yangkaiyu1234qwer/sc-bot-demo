package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.TilePosition;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Supplies;
import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;
import com.yangky.scbotdemo.util.Properties;
import com.yangky.scbotdemo.util.Times;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    // ... existing code ...
    @Override
    public void onFrame(Integer frame) {
        if (properties.isWallOffOn() && !WallOffExecutor.isWallOffFinished()) {
            return;
        }
        Player player = Games.game.self();
        if (player.supplyTotal() >= 400 || player.supplyTotal() < 52) {
            return;
        }
        int supplyDeficit = Supplies.supplyDeficit(player);
        if (supplyDeficit > 0) {
            List<RegionType> regionTypeList = new ArrayList<>();
            regionTypeList.add(RegionType.EDGE);
            TilePosition buildPos = BuildingPlacer.findPosition(UnitType.Terran_Supply_Depot, regionTypeList, 0, 0, true);
            String idempotentNo = "supply_from_" + player.supplyTotal() + "_to_" + (player.supplyTotal() + 8 * supplyDeficit);
            BuildExecutor.add(Task.ofSupplyDepot(idempotentNo, buildPos));
        }
    }

}
