package com.campus.express.vo.stats;

import lombok.Builder;
import lombok.Data;

/**
 * 站点吞吐量统计
 */
@Data
@Builder
public class StationThroughputVO {

    private Long stationId;
    private String stationName;
    private Long inboundCount;
    private Long takenCount;
    private Long overdueCount;
}
