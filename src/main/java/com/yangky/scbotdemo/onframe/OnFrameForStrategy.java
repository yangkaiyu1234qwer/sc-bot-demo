package com.yangky.scbotdemo.onframe;

import com.yangky.scbotdemo.bwem.strategy.StrategyChain;
import org.springframework.stereotype.Component;

@Component
public class OnFrameForStrategy extends OnFrame {

    @Override
    public Integer getInterval() {
        return 10;
    }

    @Override
    public void onFrame(Integer frame) {
        StrategyChain.dispatch(frame);
    }
}
