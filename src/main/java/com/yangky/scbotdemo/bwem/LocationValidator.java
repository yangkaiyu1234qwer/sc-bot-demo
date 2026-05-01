package com.yangky.scbotdemo.bwem;


import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

import java.util.List;

/**
 * LocationValidator - 统一的位置验证器
 * 所有建筑选址和建造前都使用此验证器，确保标准一致
 *
 * @author yangky
 * @Date 2026/4/30
 */
public class LocationValidator {

    /**
     * 统一的位置验证入口
     *
     * @param pos      待验证的地块位置
     * @param building 建筑类型
     * @return 是否可用
     */
    public static boolean isValid(TilePosition pos, UnitType building) {
        if (pos == null || building == null) {
            return false;
        }
        if (!isInMapBounds(pos, building)) {
            System.out.println("[LocationValidator] 超出地图边界: " + pos);
            return false;
        }
        if (building != UnitType.Terran_Refinery) {
            if (!isBuildableTerrain(pos, building)) {
                System.out.println("[LocationValidator] 地形不可建造: " + pos);
                return false;
            }
            if (!hasNoResourcesNearby(pos, building)) {
                System.out.println("[LocationValidator] 附近有资源: " + pos);
                return false;
            }
        } else {
            if (!hasGeyserAt(pos)) {
                System.out.println("[LocationValidator] 位置没有气矿: " + pos);
                return false;
            }
        }
        if (!hasNoBuildings(pos, building)) {
            System.out.println("[LocationValidator] 有建筑占用: " + pos);
            return false;
        }

        return true;
    }


    private static boolean hasGeyserAt(TilePosition pos) {
        List<Unit> units = Games.game.getUnitsOnTile(pos);
        for (Unit unit : units) {
            UnitType type = unit.getType();
            if (type == UnitType.Resource_Vespene_Geyser) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查位置是否在地图范围内
     */
    private static boolean isInMapBounds(TilePosition pos, UnitType building) {
        int mapWidth = Games.game.mapWidth();
        int mapHeight = Games.game.mapHeight();
        int buildWidth = building.tileWidth();
        int buildHeight = building.tileHeight();

        return pos.getX() >= 0
                && pos.getY() >= 0
                && pos.getX() + buildWidth <= mapWidth
                && pos.getY() + buildHeight <= mapHeight;
    }

    /**
     * 检查地形是否可建造
     */
    private static boolean isBuildableTerrain(TilePosition pos, UnitType building) {
        int buildWidth = building.tileWidth();
        int buildHeight = building.tileHeight();

        for (int dx = 0; dx < buildWidth; dx++) {
            for (int dy = 0; dy < buildHeight; dy++) {
                TilePosition tile = new TilePosition(pos.getX() + dx, pos.getY() + dy);
                if (!Games.isBuildable(tile)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查周围是否有资源（水晶矿/气矿）
     * 排除范围：建筑占据区域向外扩展2格
     */
    private static boolean hasNoResourcesNearby(TilePosition pos, UnitType building) {
        int checkRange = 1;
        int buildWidth = building.tileWidth();
        int buildHeight = building.tileHeight();
        for (int dx = -checkRange; dx <= buildWidth + checkRange; dx++) {
            for (int dy = -checkRange; dy <= buildHeight + checkRange; dy++) {
                TilePosition checkTile = new TilePosition(pos.getX() + dx, pos.getY() + dy);

                List<Unit> units = Games.game.getUnitsOnTile(checkTile);
                for (Unit unit : units) {
                    UnitType type = unit.getType();
                    if (type.isResourceContainer()
                            || type == UnitType.Resource_Mineral_Field
                            || type == UnitType.Resource_Mineral_Field_Type_2
                            || type == UnitType.Resource_Mineral_Field_Type_3
                            || type == UnitType.Resource_Vespene_Geyser) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasNoBuildings(TilePosition pos, UnitType building) {
        int buildWidth = building.tileWidth();
        int buildHeight = building.tileHeight();

        List<bwapi.Unit> allUnits = Games.game.getAllUnits();
        for (bwapi.Unit unit : allUnits) {
            if (!unit.getType().isBuilding()) {
                continue;
            }

            if (unit.getType().isResourceContainer() ||
                    unit.getType() == UnitType.Resource_Vespene_Geyser ||
                    unit.getType() == UnitType.Resource_Mineral_Field ||
                    unit.getType() == UnitType.Resource_Mineral_Field_Type_2 ||
                    unit.getType() == UnitType.Resource_Mineral_Field_Type_3) {
                continue;
            }

            if (!unit.isCompleted() && !unit.isBeingConstructed()) {
                continue;
            }

            TilePosition otherPos = unit.getTilePosition();
            UnitType otherType = unit.getType();
            int otherWidth = otherType.tileWidth();
            int otherHeight = otherType.tileHeight();

            if (isOverlapping(pos.getX(), pos.getY(), buildWidth, buildHeight,
                    otherPos.getX(), otherPos.getY(), otherWidth, otherHeight)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查两个矩形是否重叠
     */
    private static boolean isOverlapping(int x1, int y1, int w1, int h1,
                                         int x2, int y2, int w2, int h2) {
        return !(x1 + w1 <= x2 || x2 + w2 <= x1 || y1 + h1 <= y2 || y2 + h2 <= y1);
    }
}
