package com.yangky.scbotdemo.listner.created;

import bwapi.Unit;
import org.springframework.beans.factory.InitializingBean;

/**
 * UnitTrainedListner
 *
 * @author yangky
 * @Date 2026/4/28 0:15
 */
public abstract class UnitCreatedListener implements InitializingBean {

    public abstract boolean apply(Unit unit);

    public abstract void onUnitTrained(Unit unit);

    @Override
    public void afterPropertiesSet() {
        UnitCreatedListenerChain.addListener(this);
    }

}
