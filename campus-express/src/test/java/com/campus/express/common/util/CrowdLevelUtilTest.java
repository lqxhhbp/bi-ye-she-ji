package com.campus.express.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrowdLevelUtilTest {

    @Test
    void shouldMapScoreBoundaries() {
        assertEquals(0, CrowdLevelUtil.scoreToLevel(32.99));
        assertEquals(1, CrowdLevelUtil.scoreToLevel(33));
        assertEquals(1, CrowdLevelUtil.scoreToLevel(66));
        assertEquals(2, CrowdLevelUtil.scoreToLevel(66.01));
    }
}
