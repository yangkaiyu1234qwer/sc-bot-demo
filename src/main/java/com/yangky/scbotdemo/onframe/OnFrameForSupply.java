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
        if (player.supplyTotal() >= 400) {
            return;
        }
        int supplyDeficit = Supplies.supplyDeficit(player);
        if (supplyDeficit > 0) {
            UnitType supplyBuilding = Supplies.getSupplyUnitType(player);
            Unit mainBase = Bases.getMainBaseUnit();
            if (mainBase == null) return;

            // ✅ 优先使用 EdgePosition，失败后降级到 CentralPosition
            TilePosition buildPos = Positions.getEdgePosition(supplyBuilding, mainBase);
            if (buildPos == null) {
                System.out.println("[Supply] EdgePosition 失败，尝试 CentralPosition");
                buildPos = Positions.getCentralPosition(supplyBuilding, mainBase.getTilePosition());
            }
            if (buildPos == null) {
                System.out.println("[Supply] 所有选址方案均失败，使用 BWAPI 默认方法");
                buildPos = Games.game.getBuildLocation(supplyBuilding, mainBase.getTilePosition(), 20);
            }
            if (buildPos == null) {
                System.out.println("[Supply] ✗ 无法找到 Supply 位置！");
                return;
            }

            String idempotentNo = "supply_from_" + player.supplyTotal() + "_to_" + (player.supplyTotal() + 8 * supplyDeficit);
            Builds.add(new BuildTask(idempotentNo, buildPos, supplyBuilding));
        }
    }

}
