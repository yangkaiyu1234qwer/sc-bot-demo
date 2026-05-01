package com.yangky.scbotdemo.listner.destroy;

import bwapi.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * UnitDestoryListenerChain
 *
 * @author yangky
 * @Date 2026/4/29 15:51
 */
public class UnitDestroyListenerChain {
    private static final List<UnitDestroyListener> listeners = new ArrayList<>();

    // 注册监听器
    public static void addListener(UnitDestroyListener listener) {
        listeners.add(listener);
    }

    // 事件触发 → 分发给所有 listener
    public static void dispatch(Unit unit) {
        listeners.stream().filter(e -> e.apply(unit)).findFirst().ifPresent(e -> e.onDestroy(unit));
    }
}
