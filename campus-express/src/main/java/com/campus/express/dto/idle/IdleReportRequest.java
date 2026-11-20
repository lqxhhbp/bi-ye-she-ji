package com.campus.express.dto.idle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生空闲度上报请求
 */
@Data
public class IdleReportRequest {

    @NotNull(message = "站点不能为空")
    private Long stationId;

    @NotNull(message = "拥挤档位不能为空")
    @Min(value = 0, message = "拥挤档位只能是 0、1、2")
    @Max(value = 2, message = "拥挤档位只能是 0、1、2")
    private Integer level;
}
