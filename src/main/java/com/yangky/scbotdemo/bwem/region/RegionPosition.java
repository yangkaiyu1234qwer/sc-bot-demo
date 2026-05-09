package com.yangky.scbotdemo.bwem.region;

import bwapi.TilePosition;
import bwapi.Unit;
import com.yangky.scbotdemo.bwem.Games;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * RegionPosition
 *
 * @author yangky
 * @Date 2026/5/9 17:09
 */
@Data
public class RegionPosition {
    private int x;
    private int y;
    private RegionType type;
    private boolean isBuildable;
    private boolean isMineral;
    private boolean isGeyser;
    private boolean isChokePoint;

    public static RegionPosition outBorder(TilePosition pos, Set<TilePosition> chokePointPositions) {
        RegionPosition border = new RegionPosition();
        border.setX(pos.getX());
        border.setY(pos.getY());
        border.setType(RegionType.BOUNDARY);
        border.setBuildable(Games.game.isBuildable(pos));
        border.mineralOrGasMark(pos);
        border.setChokePoint(isChokePoint(pos, chokePointPositions));
        return border;
    }

    public void mineralOrGasMark(TilePosition pos) {
        List<Unit> units = Games.game.getUnitsOnTile(pos);
        for (bwapi.Unit unit : units) {
            bwapi.UnitType type = unit.getType();
            if (type == bwapi.UnitType.Resource_Mineral_Field ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_2 ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_3
            ) {
                this.setMineral(true);
            } else if (type == bwapi.UnitType.Resource_Vespene_Geyser) {
                this.setGeyser(true);
            }
        }
    }

    public static boolean isMineralOrGas(TilePosition pos) {
        List<Unit> units = Games.game.getUnitsOnTile(pos);
        for (bwapi.Unit unit : units) {
            bwapi.UnitType type = unit.getType();
            if (type == bwapi.UnitType.Resource_Mineral_Field ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_2 ||
                    type == bwapi.UnitType.Resource_Mineral_Field_Type_3 ||
                    type == bwapi.UnitType.Resource_Vespene_Geyser) {
                return true;
            }
        }
        return false;
    }


    public static boolean isChokePoint(TilePosition pos, Set<TilePosition> chokePointPositions) {
        return chokePointPositions.contains(pos);
    }
}
