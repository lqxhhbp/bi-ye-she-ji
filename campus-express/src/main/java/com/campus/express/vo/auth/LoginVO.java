package com.campus.express.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录结果
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private UserVO user;
}
