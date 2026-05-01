package com.yangky.scbotdemo.bwem.walloff;

import bwapi.TilePosition;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * WallOffConfig
 *
 * @author yangky
 * @Date 2026/4/29 19:58
 */
public class WallOffConfig {
    @Getter
    private static final Map<TilePosition, WallOff> map = new HashMap<>();

    static {
        // 1点
        WallOff point1 = new WallOff();
        point1.getList().add(WallOffBuilding.firstSupply(new TilePosition(96, 21)));
        point1.getList().add(WallOffBuilding.firstBarrack(new TilePosition(103, 17)));
        point1.getList().add(WallOffBuilding.secondSupply(null));
        point1.getList().add(WallOffBuilding.bunker(new TilePosition(99, 20)));
        point1.getList().add(WallOffBuilding.secondBarrack(new TilePosition(98, 23)));

        // 3点
        WallOff point3 = new WallOff();
        point3.getList().add(WallOffBuilding.firstSupply(new TilePosition(101, 61)));
        point3.getList().add(WallOffBuilding.firstBarrack(new TilePosition(108, 59)));
        point3.getList().add(WallOffBuilding.secondSupply(null));
        point3.getList().add(WallOffBuilding.bunker(new TilePosition(104, 61)));
        point3.getList().add(WallOffBuilding.secondBarrack(new TilePosition(103, 63)));

        // 5点
        WallOff point5 = new WallOff();
        point5.getList().add(WallOffBuilding.firstSupply(new TilePosition(96, 99)));
        point5.getList().add(WallOffBuilding.firstBarrack(new TilePosition(94, 101)));
        point5.getList().add(WallOffBuilding.secondSupply(new TilePosition(97, 97)));
        point5.getList().add(WallOffBuilding.bunker(new TilePosition(98, 101)));

        // 6点
        WallOff point6 = new WallOff();
        point6.getList().add(WallOffBuilding.firstSupply(new TilePosition(55, 94)));
        point6.getList().add(WallOffBuilding.firstBarrack(new TilePosition(52, 96)));
        point6.getList().add(WallOffBuilding.secondSupply(null));
        point6.getList().add(WallOffBuilding.bunker(new TilePosition(56, 96)));

        // 7点
        WallOff point7 = new WallOff();
        point7.getList().add(WallOffBuilding.firstSupply(new TilePosition(17, 93)));
        point7.getList().add(WallOffBuilding.firstBarrack(new TilePosition(18, 95)));
        point7.getList().add(WallOffBuilding.secondSupply(null));
        point7.getList().add(WallOffBuilding.bunker(new TilePosition(15, 95)));

        // 9点
        WallOff point9 = new WallOff();
        point9.getList().add(WallOffBuilding.firstSupply(new TilePosition(35, 57)));
        point9.getList().add(WallOffBuilding.firstBarrack(new TilePosition(36, 59)));
        point9.getList().add(WallOffBuilding.secondSupply(null));
        point9.getList().add(WallOffBuilding.bunker(new TilePosition(33, 59)));

        // 11点
        WallOff point11 = new WallOff();
        point11.getList().add(WallOffBuilding.firstSupply(new TilePosition(31, 20)));
        point11.getList().add(WallOffBuilding.firstBarrack(new TilePosition(22, 20)));
        point11.getList().add(WallOffBuilding.secondSupply(null));
        point11.getList().add(WallOffBuilding.bunker(new TilePosition(28, 20)));
        point11.getList().add(WallOffBuilding.secondBarrack(new TilePosition(28, 22)));

        // 12点
        WallOff point12 = new WallOff();
        point12.getList().add(WallOffBuilding.firstSupply(new TilePosition(50, 24)));
        point12.getList().add(WallOffBuilding.firstBarrack(new TilePosition(56, 17)));
        point12.getList().add(WallOffBuilding.secondSupply(null));
        point12.getList().add(WallOffBuilding.bunker(new TilePosition(54, 23)));
        point12.getList().add(WallOffBuilding.secondBarrack(new TilePosition(52, 26)));

        map.put(point1.getList().get(1).getTilePosition(), point1);
        map.put(point3.getList().get(1).getTilePosition(), point3);
        map.put(point5.getList().get(1).getTilePosition(), point5);
        map.put(point6.getList().get(1).getTilePosition(), point6);
        map.put(point7.getList().get(1).getTilePosition(), point7);
        map.put(point9.getList().get(1).getTilePosition(), point9);
        map.put(point11.getList().get(1).getTilePosition(), point11);
        map.put(point12.getList().get(1).getTilePosition(), point12);
    }
}
