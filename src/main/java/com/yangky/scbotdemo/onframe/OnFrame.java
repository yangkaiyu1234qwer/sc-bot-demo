package com.yangky.scbotdemo.onframe;

import org.springframework.beans.factory.InitializingBean;

/**
 * Onframe
 *
 * @author yangky
 * @Date 2026/4/28 4:15
 */
public abstract class OnFrame implements InitializingBean {

    public boolean apply(Integer frame) {
        return frame % getInterval() == 0;
    }

    public abstract Integer getInterval();

    public abstract void onFrame(Integer frame);

    @Override
    public void afterPropertiesSet() {
        OnFrameChain.addOnFrame(this);
    }
}
