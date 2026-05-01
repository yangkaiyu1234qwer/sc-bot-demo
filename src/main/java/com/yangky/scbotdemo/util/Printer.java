package com.yangky.scbotdemo.util;

import bwapi.*;

import java.util.List;

/**
 * Printer
 *
 * @author yangky
 * @Date 2026/4/29 14:08
 */
public class Printer {

    public static void printBuildingPositions(Game game) {
        if (game == null) {
            System.out.println("游戏对象为空，无法打印建筑坐标");
            return;
        }

        Player self = game.self();
        if (self == null) {
            System.out.println("玩家对象为空，无法打印建筑坐标");
            return;
        }

        System.out.println("\n========================================");
        System.out.println("========== 建筑坐标汇总 ==========");
        System.out.println("========================================\n");

        List<Unit> allUnits = self.getUnits();
        if (allUnits == null || allUnits.isEmpty()) {
            System.out.println("未检测到任何单位");
            return;
        }

        int barracksCount = 0;
        int supplyCount = 0;
        int bunkerCount = 0;

        for (Unit unit : allUnits) {
            if (unit == null || !unit.exists() || !unit.isCompleted()) {
                continue;
            }

            try {
                TilePosition tilePos = unit.getTilePosition();
                Position pos = unit.getPosition();

                if (unit.getType() == bwapi.UnitType.Terran_Barracks) {
                    barracksCount++;
                    System.out.printf("兵营 #%d - 地块坐标: (%d, %d), 像素坐标: (%d, %d)%n",
                            barracksCount, tilePos.getX(), tilePos.getY(), pos.getX(), pos.getY());
                } else if (unit.getType() == bwapi.UnitType.Terran_Supply_Depot) {
                    supplyCount++;
                    System.out.printf("补给站 #%d - 地块坐标: (%d, %d), 像素坐标: (%d, %d)%n",
                            supplyCount, tilePos.getX(), tilePos.getY(), pos.getX(), pos.getY());
                } else if (unit.getType() == bwapi.UnitType.Terran_Bunker) {
                    bunkerCount++;
                    System.out.printf("地堡 #%d - 地块坐标: (%d, %d), 像素坐标: (%d, %d)%n",
                            bunkerCount, tilePos.getX(), tilePos.getY(), pos.getX(), pos.getY());
                }
            } catch (Exception e) {
                // 忽略单个单位的异常，继续处理其他单位
                System.err.println("处理单位时出错: " + e.getMessage());
            }
        }

        System.out.println("\n----------------------------------------");
        System.out.printf("统计: 兵营=%d, 补给站=%d, 地堡=%d%n",
                barracksCount, supplyCount, bunkerCount);
        System.out.printf("总计: %d 个建筑%n", barracksCount + supplyCount + bunkerCount);
        System.out.println("========================================\n");
    }
}
