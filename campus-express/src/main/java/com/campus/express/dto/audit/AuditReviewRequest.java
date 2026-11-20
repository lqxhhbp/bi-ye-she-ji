package com.campus.express.dto.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 站长入驻审核请求
 */
@Data
public class AuditReviewRequest {

    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果只能是 1 通过或 2 驳回")
    @Max(value = 2, message = "审核结果只能是 1 通过或 2 驳回")
    private Integer status;
}
