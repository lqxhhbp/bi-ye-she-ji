package com.campus.express.security;

/**
 * JWT 中保存的最小登录身份
 */
public record AuthUser(Long userId, Integer role, Long stationId) {
}
