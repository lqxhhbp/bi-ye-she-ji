package com.campus.express.vo.stats;

import lombok.Builder;
import lombok.Data;

/**
 * 操作员工作量统计
 */
@Data
@Builder
public class OperatorWorkloadVO {

    private Long userId;
    private String realName;
    private String stationName;
    private Long inboundCount;
    private Long verifiedCount;
}
