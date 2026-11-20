package com.campus.express.vo.stats;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * 站点每日统计
 */
@Data
@AllArgsConstructor
public class StationDailyVO {

    private LocalDate date;
    private Long inboundCount;
    private Long takenCount;
    private Long revokedCount;
}
