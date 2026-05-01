package com.yangky.scbotdemo.listner.created;

import bwapi.Unit;

import java.util.ArrayList;
import java.util.List;

// 责任链：管理所有监听器
public class UnitCreatedListenerChain {
    private static final List<UnitCreatedListener> listeners = new ArrayList<>();

    // 注册监听器
    public static void addListener(UnitCreatedListener listener) {
        listeners.add(listener);
    }

    // 事件触发 → 分发给所有 listener
    public static void dispatch(Unit unit) {
        listeners.stream().filter(e -> e.apply(unit)).findFirst().ifPresent(e -> e.onUnitTrained(unit));
    }
}