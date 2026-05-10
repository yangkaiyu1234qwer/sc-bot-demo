package com.yangky.scbotdemo.bwem.build;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TaskStatus {
    RETRYING(-1),           // 重试中

    WAITING(0),             // 等待中

    ASSIGN_POSITION(1),     // 指定坐标

    ASSIGN_SCV(2),          // 指派SCV

    MOVING(3),              // 移动中

    PRE_BUILD(4),           // 准备建造

    CONSTRUCTING(5),       // 正在建造

    COMPLETED(6),          // 已完成

    FAILED(7),             // 已失败
    ;


    @Getter
    private final int code;

}
