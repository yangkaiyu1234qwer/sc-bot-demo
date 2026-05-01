package com.yangky.scbotdemo.bwem.strategy;

import com.yangky.scbotdemo.util.Properties;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * TurtleToLeviathanStrategy
 *
 * @author yangky
 * @Date 2026/4/29 16:25
 */
@Component
@AllArgsConstructor
public class TurtleToLeviathanStrategy extends AbstractStrategy {

    private Properties properties;

    public boolean apply() {
        return StringUtils.equals(properties.getStrategyPolicy(), "Turtle_To_Leviathan");
    }

    @Override
    public void execute(Integer frame) {
    }

}
