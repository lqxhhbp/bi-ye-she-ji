package com.campus.express.common.enums;

import lombok.Getter;

/**
 * 系统角色
 */
@Getter
public enum Role {

    STUDENT(0, "学生"),
    STATION_ADMIN(1, "站点管理员"),
    SUPER_ADMIN(2, "超级管理员");

    private final int code;
    private final String description;

    Role(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
