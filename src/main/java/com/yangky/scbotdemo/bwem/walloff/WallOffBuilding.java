package com.yangky.scbotdemo.bwem.walloff;

import bwapi.Game;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.BuildTiming;
import com.yangky.scbotdemo.Callback;
import com.yangky.scbotdemo.bwem.Builds;
import com.yangky.scbotdemo.bwem.Units;
import lombok.Data;
import lombok.ToString;

/**
 * WallOffBuilding
 *
 * @author yangky
 * @Date 2026/4/29 21:28
 */

@Data
@ToString(exclude = {"callback", "buildTiming"})
public class WallOffBuilding {
    private String idempotentNo;
    private UnitType unitType;
    private TilePosition tilePosition;
    private BuildTiming buildTiming;
    private Callback callback;

    public static WallOffBuilding firstSupply(TilePosition tilePosition) {
        WallOffBuilding wallOffBuilding = new WallOffBuilding(UnitType.Terran_Supply_Depot, tilePosition, WallOffBuilding::firstSupplyTiming);
        wallOffBuilding.idempotentNo = "firstSupply";
        return wallOffBuilding;
    }

    public static WallOffBuilding firstBarrack(TilePosition tilePosition) {
        WallOffBuilding wallOffBuilding = new WallOffBuilding(UnitType.Terran_Barracks, tilePosition, WallOffBuilding::firstBarrackTiming);
        wallOffBuilding.idempotentNo = "firstBarrack";
        return wallOffBuilding;
    }

    public static WallOffBuilding bunker(TilePosition tilePosition) {
        WallOffBuilding wallOffBuilding = new WallOffBuilding(UnitType.Terran_Bunker, tilePosition, WallOffBuilding::bunkerTiming);
        wallOffBuilding.idempotentNo = "firstBunker";
        // ✅ 不再设置 callback，由 WallOffExecutor 统一处理
        return wallOffBuilding;
    }

    public static WallOffBuilding secondSupply(TilePosition tilePosition) {
        WallOffBuilding wallOffBuilding = new WallOffBuilding(UnitType.Terran_Supply_Depot, tilePosition, WallOffBuilding::secondSupplyTiming);
        wallOffBuilding.idempotentNo = "secondSupply";
        // ✅ 不再设置 callback
        return wallOffBuilding;
    }

    public static WallOffBuilding secondBarrack(TilePosition tilePosition) {
        WallOffBuilding wallOffBuilding = new WallOffBuilding(UnitType.Terran_Barracks, tilePosition, WallOffBuilding::secondBarrackTiming);
        wallOffBuilding.idempotentNo = "secondBarrack";
        // ✅ 不再设置 callback
        return wallOffBuilding;
    }


    public WallOffBuilding(UnitType unitType, TilePosition tilePosition, BuildTiming buildTiming) {
        this.unitType = unitType;
        this.tilePosition = tilePosition;
        this.buildTiming = buildTiming;
    }

    public WallOffBuilding(UnitType unitType, TilePosition tilePosition, BuildTiming buildTiming, Callback callback) {
        this.unitType = unitType;
        this.tilePosition = tilePosition;
        this.buildTiming = buildTiming;
        this.callback = callback;
    }

    private static boolean firstSupplyTiming(Game game) {
        int supplyUsed = game.self().supplyUsed();
        long supplyBuilding = Builds.getCountByBuildingType(UnitType.Terran_Supply_Depot);
        int supplyDepotExisted = Units.getSelfUnits(UnitType.Terran_Supply_Depot).size();
/*        System.out.println("supplyUsed=" + supplyUsed
                + "，supplyBuilding=" + supplyBuilding
                + "，supplyDepotExisted=" + supplyDepotExisted);*/
        return supplyUsed >= 16               // 8 人口 造第一个房子
                && supplyBuilding < 1 // 没有正在建造的补给站
                && supplyDepotExisted < 1;
    }

    private static boolean firstBarrackTiming(Game game) {
        int supplyUsed = game.self().supplyUsed();
        int barracksBuilding = Builds.getCountByBuildingType(UnitType.Terran_Barracks);
        int barrackExisted = Units.getSelfUnits(UnitType.Terran_Barracks).size();
//        System.out.println("supplyUsed=" + supplyUsed
//                + "，barracksExisted=" + barracksBuilding
//                + "，bunkersBuilding=" + barrackExisted);
        return supplyUsed >= 20               // 10 人口 造第一个兵营
                && barracksBuilding < 1// 没有正在建造的兵营
                && barrackExisted < 1
                && game.self().minerals() > 110;
    }

    private static boolean bunkerTiming(Game game) {
        int supplyUsed = game.self().supplyUsed();
        // 造地堡需要兵营已建造
        long barrackCompleted = Units.getSelfUnits(UnitType.Terran_Barracks).stream().filter(Unit::isCompleted).count();
        int bunkersBuilding = Builds.getCountByBuildingType(UnitType.Terran_Bunker);
        long bunkersExisted = Units.getSelfUnits(UnitType.Terran_Bunker).size();
//        System.out.println("supplyUsed=" + supplyUsed
//                + "，barracksExisted=" + barrackCompleted
//                + "，bunkersBuilding=" + bunkersBuilding
//                + "，bunkersExisted=" + bunkersExisted);
        return supplyUsed >= 30               // 15 人口 造第地堡
                && barrackCompleted >= 1       // 兵营已完成
                && bunkersBuilding < 1        // 没有正在建造的地堡
                && bunkersExisted < 1;
    }

    private static Boolean secondSupplyTiming(Game game) {
        int supplyUsed = game.self().supplyUsed();
        int barracksExisted = Units.getSelfUnits(UnitType.Terran_Barracks).size();
        int supplyExisted = Units.getSelfUnits(UnitType.Terran_Supply_Depot).size();
//        System.out.println("supplyUsed=" + game.self().supplyUsed() + "，barracksExisted=" + barracksExisted + "，supplyExisted=" + supplyExisted);
        return supplyUsed >= 14               // 12 人口 造第2个补给站
                && barracksExisted >= 1// 兵营已完成
                && supplyExisted < 2 // 没有第2个补给站
                ;
    }

    private static boolean secondBarrackTiming(Game game) {
        int supplyUsed = game.self().supplyUsed();
        int barracksBuilding = Builds.getCountByBuildingType(UnitType.Terran_Barracks);
        int barrackExisted = Units.getSelfUnits(UnitType.Terran_Barracks).size();
//        System.out.println("supplyUsed=" + supplyUsed
//                + "，barracksBuilding=" + barracksBuilding
//                + "，barrackExisted=" + barrackExisted);
        return supplyUsed >= 28               // 14 人口 造第二个兵营
                && barracksBuilding < 1       // 没有正在建造的兵营
                && barrackExisted <= 1;       // 没有第二个兵营
    }

}
