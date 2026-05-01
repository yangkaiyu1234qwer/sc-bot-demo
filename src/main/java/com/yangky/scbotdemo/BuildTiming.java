package com.yangky.scbotdemo;

import bwapi.Game;

/**
 * BuildTiming
 *
 * @author yangky
 * @Date 2026/4/29 21:41
 */
@FunctionalInterface
public interface BuildTiming {
    Boolean accept(Game game);
}
