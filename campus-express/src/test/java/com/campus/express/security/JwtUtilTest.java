package com.campus.express.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-at-least-thirty-two-bytes-long-123456");
        ReflectionTestUtils.setField(jwtUtil, "expirationMinutes", 60L);
        jwtUtil.init();
    }

    @Test
    void shouldRoundTripUserClaims() {
        String token = jwtUtil.generateToken(7L, 1, 3L);

        AuthUser user = jwtUtil.parseToken(token);

        assertEquals(7L, user.userId());
        assertEquals(1, user.role());
        assertEquals(3L, user.stationId());
    }

    @Test
    void shouldKeepStudentStationAsNull() {
        String token = jwtUtil.generateToken(8L, 0, null);

        AuthUser user = jwtUtil.parseToken(token);

        assertEquals(8L, user.userId());
        assertEquals(0, user.role());
        assertNull(user.stationId());
    }
}
