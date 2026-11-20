package com.campus.express.common.util;

/**
 * 拥挤度档位工具
 */
public final class CrowdLevelUtil {

    private CrowdLevelUtil() {
    }

    public static String levelName(Integer level) {
        if (level == null) {
            return "未知";
        }
        return switch (level) {
            case 0 -> "空闲";
            case 1 -> "一般";
            case 2 -> "拥挤";
            default -> "未知";
        };
    }

    public static double levelValue(Integer level) {
        if (level == null) {
            return 0.5;
        }
        return switch (level) {
            case 0 -> 0.1;
            case 1 -> 0.5;
            case 2 -> 0.9;
            default -> 0.5;
        };
    }

    public static int scoreToLevel(double score) {
        if (score < 33) {
            return 0;
        }
        if (score <= 66) {
            return 1;
        }
        return 2;
    }
}
