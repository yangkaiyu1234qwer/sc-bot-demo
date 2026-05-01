package com.yangky.scbotdemo.util;

import com.yangky.scbotdemo.bwem.Games;

import java.math.BigDecimal;

/**
 * Times
 *
 * @author yangky
 * @Date 2026/4/28 4:06
 */
public class Times {

    // 每帧约40毫秒
    public static final Integer BUILD_INTERVAL = 10;

    // 每20帧确认1次补给
    public static final Integer SUPPLY_INTERVAL = 5;

    // 每30帧做一次经济运营
    public static final Integer ECONOMIC_INTERVAL = 10;

    // 每10帧做一次战斗规划
    public static final Integer BATTLE_PLAN_INTERVAL = 10;

    public static Integer secondsFromStart() {
        return BigDecimal.valueOf(Maths.sub(System.currentTimeMillis(), Games.start)).movePointLeft(3).intValue();
    }
}
