package com.yangky.scbotdemo.bwem;

import bwapi.Player;
import bwapi.UnitType;

/**
 * SupplyUtils
 *
 * @author yangky
 * @Date 2026/4/27 20:37
 */
public class Supplies {
    private static Integer maxSupplyUsed = 0;

    public static void maxSupplyUsedIncrement(int used) {
        maxSupplyUsed += used;
    }

    public static Integer getMaxSupplyUsed() {
        return maxSupplyUsed;
    }

    public static Boolean isSupplyEnough(Integer supply) {
        Player self = Games.game.self();
        return supply + self.supplyUsed() < self.supplyTotal();
    }

    public static int supplyDeficit(Player self) {
        int supplyUsed = self.supplyUsed();
        int supplyTotal = self.supplyTotal();
        int deficit = 0;
        if (supplyUsed < 16) {
            // 人口规模很小，不需要补
            return 0;
        } else if (maxSupplyUsed <= 17) {
            // 人口规模小，补1个
            deficit = 1;
        } else if (maxSupplyUsed <= 25) {
            // 人口规模中等，当剩余人口不足8时补1个
            deficit = supplyTotal - supplyUsed <= 4 ? 1 : 0;
        } else if (maxSupplyUsed <= 200) {
            deficit = supplyTotal - supplyUsed <= 8 ? 1 : 0;
        } else {
            // 人口规模大，更激进地补充
            deficit = supplyTotal - supplyUsed <= 12 ? 2 : 0;
        }
        // 减去已经在建造的补给站数量
        int buildingCount = Builds.getCountByBuildingType(getSupplyUnitType(self));
        deficit = Math.max(deficit - buildingCount, 0);
        if (deficit > 0) {
            System.out.println("[补给站] 触发建造");
        }
        return deficit;
    }


    public static UnitType getSupplyUnitType(Player player) {
        switch (player.getRace()) {
            case Terran:
                return UnitType.Terran_Supply_Depot;
            case Protoss:
                return UnitType.Protoss_Pylon;
            case Zerg:
                return UnitType.Zerg_Overlord;
            default:
                return null;
        }
    }

    public static void reset() {
        maxSupplyUsed = 0;
    }
}
