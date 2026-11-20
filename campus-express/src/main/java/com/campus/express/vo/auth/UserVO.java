package com.campus.express.vo.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 当前用户信息
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String phone;
    private String realName;
    private Integer role;
    private Long stationId;
    private Integer status;
}
