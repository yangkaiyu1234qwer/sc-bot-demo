package com.yangky.scbotdemo.bwem.build;

import org.springframework.beans.factory.InitializingBean;

/**
 * StateHandler
 *
 * @author yangky
 * @Date 2026/5/9 23:13
 */
public abstract class StateHandler implements InitializingBean {

    public abstract TaskStatus accessStatus();

    public abstract void process(Task task);

    @Override
    public void afterPropertiesSet() throws Exception {
        BuildExecutor.handleRegister(this);
    }
}
