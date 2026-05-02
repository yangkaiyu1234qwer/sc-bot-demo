package com.yangky.scbotdemo.onframe;

import org.springframework.stereotype.Component;

/**
 * OnFrameForDefense
 *
 * @author yangky
 * @Date 2026/5/2 17:55
 */
@Component
public class OnFrameForDefense extends OnFrame {
    @Override
    public Integer getInterval() {
        return 5;
    }

    @Override
    public void onFrame(Integer frame) {

    }
}
