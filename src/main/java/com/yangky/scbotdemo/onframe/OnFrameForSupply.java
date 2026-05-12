package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.build.BuildingDemandManager;
import com.yangky.scbotdemo.bwem.build.RegionStrategy;
import com.yangky.scbotdemo.bwem.region.RegionType;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;
import com.yangky.scbotdemo.util.Maths;
import com.yangky.scbotdemo.util.Properties;
import com.yangky.scbotdemo.util.Times;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

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
//        if (properties.isWallOffOn() && !WallOffExecutor.isWallOffFinished()) {
//            return;
//        }
        Player player = Games.game.self();
        if (player.supplyTotal() >= 400 || player.supplyTotal() < 52) {
            return;
        }
//        int supplyDeficit = Supplies.supplyDeficit(player);
//        if (supplyDeficit > 0) {
//            List<RegionType> regionTypeList = new ArrayList<>();
//            regionTypeList.add(RegionType.EDGE);
//            TilePosition buildPos = BuildingPlacer.findPosition(UnitType.Terran_Supply_Depot, regionTypeList, 0, 0, true);
//            String idempotentNo = "supply_from_" + player.supplyTotal() + "_to_" + (player.supplyTotal() + 8 * supplyDeficit);
//            BuildExecutor.add(Task.ofSupplyDepot(idempotentNo, buildPos));
//        }
        // 当前人口总数
        int supplyTotal = player.supplyTotal();
        int supplyUsed = player.supplyUsed();
        if (Maths.mul(supplyUsed, 100) >= Maths.mul(supplyTotal, 75)) {
            int demand = (int) (Maths.div(supplyTotal - 20, 16, 1, RoundingMode.CEILING) + 1);
            BuildingDemandManager.declare(UnitType.Terran_Supply_Depot, demand, RegionStrategy.forTypes(RegionType.EDGE));
        }
    }

}
