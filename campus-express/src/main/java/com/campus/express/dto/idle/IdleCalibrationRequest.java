package com.campus.express.dto.idle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 站长空闲度校准请求
 */
@Data
public class IdleCalibrationRequest {

    @NotNull(message = "拥挤档位不能为空")
    @Min(value = 0, message = "拥挤档位只能是 0、1、2")
    @Max(value = 2, message = "拥挤档位只能是 0、1、2")
    private Integer level;
}
