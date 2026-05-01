package com.yangky.scbotdemo.onframe;

import org.springframework.stereotype.Component;

/**
 * OnFrameTank
 *
 * @author yangky
 * @Date 2026/5/1 13:51
 */
@Component
public class OnFrameForTank extends OnFrame {
    @Override
    public Integer getInterval() {
        return 25;
    }

    @Override
    public void onFrame(Integer frame) {
    }
}
