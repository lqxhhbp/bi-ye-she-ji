package com.campus.express.vo.idle;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空闲度上报展示信息
 */
@Data
@Builder
public class IdleReportVO {

    private Long id;
    private Long stationId;
    private String stationName;
    private Integer level;
    private String levelName;
    private Integer source;
    private LocalDateTime createdAt;
}
