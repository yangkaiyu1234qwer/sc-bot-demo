package com.yangky.scbotdemo.bwem.build;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TaskStatus {
    RETRYING(-1),           // 重试中

    WAITING(0),            // 等待中

    SCV_ASSIGNED(1),       // 已指派SCV

    POSITION_ASSIGNED(2),  // 已指定坐标

    MOVING(3),             // 进行中

    BUILDING(4),           // 建造中

    COMPLETED(5),          // 已完成
    ;


    @Getter
    private final int code;

}
