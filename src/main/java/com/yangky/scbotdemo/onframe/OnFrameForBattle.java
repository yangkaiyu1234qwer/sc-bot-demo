package com.yangky.scbotdemo.onframe;

import com.yangky.scbotdemo.util.Times;
import org.springframework.stereotype.Component;

/**
 * BattleOnFrame
 *
 * @author yangky
 * @Date 2026/4/28 4:19
 */
@Component
public class OnFrameForBattle extends OnFrame {
    @Override
    public Integer getInterval() {
        return Times.BATTLE_PLAN_INTERVAL;
    }

    @Override
    public void onFrame(Integer frame) {

    }
}
