package com.campus.express.vo.stats;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 拥挤度分布
 */
@Data
@AllArgsConstructor
public class CrowdDistributionVO {

    private Integer crowdLevel;
    private String crowdLevelName;
    private Long stationCount;
}
