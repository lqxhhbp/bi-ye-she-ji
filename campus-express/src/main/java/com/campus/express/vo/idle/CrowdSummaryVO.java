package com.campus.express.vo.idle;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 站点当前拥挤度概览
 */
@Data
@Builder
public class CrowdSummaryVO {

    private Long stationId;
    private String stationName;
    private Integer crowdLevel;
    private String crowdLevelName;
    private BigDecimal crowdScore;
    private LocalDateTime crowdUpdatedAt;
    private Long recentReportCount;
}
