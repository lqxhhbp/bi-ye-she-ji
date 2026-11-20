package com.campus.express.vo.idle;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 空闲度趋势点
 */
@Data
@AllArgsConstructor
public class IdleTrendPointVO {

    private LocalDateTime time;
    private BigDecimal score;
    private BigDecimal averageLevel;
    private Long reportCount;
}
