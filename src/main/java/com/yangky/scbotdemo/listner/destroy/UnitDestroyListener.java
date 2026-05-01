package com.yangky.scbotdemo.listner.destroy;

import bwapi.Unit;
import org.springframework.beans.factory.InitializingBean;

/**
 * UnitTrainedListner
 *
 * @author yangky
 * @Date 2026/4/28 0:15
 */
public abstract class UnitDestroyListener implements InitializingBean {

    public abstract boolean apply(Unit unit);

    public abstract void onDestroy(Unit unit);

    @Override
    public void afterPropertiesSet() {
        UnitDestroyListenerChain.addListener(this);
    }

}
