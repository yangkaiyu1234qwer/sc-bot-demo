package com.yangky.scbotdemo.bwem.strategy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * StratetegyChain
 *
 * @author yangky
 * @Date 2026/4/29 18:10
 */
@Component
public class StrategyChain {
    private static final List<AbstractStrategy> strategies = new ArrayList<>();

    public static void register(AbstractStrategy e) {
        strategies.add(e);
    }

    public static void dispatch(Integer frame) {
        // 前10秒不用检测
        if (frame < 250) {
            return;
        }
        strategies.forEach(e -> {
            if (e.apply()) {
                e.execute(frame);
            }
        });
    }
}
