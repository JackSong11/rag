package com.example.rag.common.enums;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {

    UNSTART("0", "未开始"),
    RUNNING("1", "运行中"),
    CANCEL("2", "已取消"),
    DONE("3", "已完成"),
    FAIL("4", "失败"),
    SCHEDULE("5", "已调度");

    private final String code;

    private final String desc;

    TaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}