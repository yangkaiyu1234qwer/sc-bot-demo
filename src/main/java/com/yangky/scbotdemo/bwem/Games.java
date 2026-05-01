package com.yangky.scbotdemo.bwem;

import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwem.BWEM;
import bwem.Base;
import bwem.Mineral;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PlayerUtils
 *
 * @author yangky
 * @Date 2026/4/27 19:49
 */
@Data
public class Games {
    public static BWEM bwem;
    public static Game game;
    public static long start;

    public static void init(BWEM bwem1, Game game1) {
        bwem = bwem1;
        game = game1;
        start = System.currentTimeMillis();
    }

    public Games(BWEM bwem, Game game) {
        Games.bwem = bwem;
        Games.game = game;
    }


    public static boolean isMineralGatherAble(Base base) {
        List<Mineral> minerals = base.getMinerals();
        return minerals.stream().anyMatch(e -> e.getAmount() > 0);
    }

    public static long mineralCount(List<Base> bases) {
        return bases.stream().mapToLong(e -> e.getMinerals().size()).sum();
    }

    public static List<Unit> getWorkers(Player player) {
        return player.getUnits().stream()
                .filter(e -> e.getType().isWorker())
                .collect(Collectors.toList());
    }


    public static boolean isBuildable(TilePosition position) {
        return game.isBuildable(position);
    }
}
