package com.yangky.scbotdemo.bwem.strategy;

import org.springframework.beans.factory.InitializingBean;

/**
 * AbstractStrategy
 *
 * @author yangky
 * @Date 2026/4/29 17:52
 */
public abstract class AbstractStrategy implements InitializingBean {

    public abstract boolean apply();

    public abstract void execute(Integer frame);

    @Override
    public void afterPropertiesSet() throws Exception {
        StrategyChain.register(this);
    }
}
