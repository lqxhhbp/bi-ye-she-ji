package com.campus.express.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 站长入驻申请
 */
@Data
public class StationApplyRequest {

    @NotNull(message = "站点不能为空")
    private Long stationId;

    @NotBlank(message = "申请理由不能为空")
    @Size(max = 500, message = "申请理由不能超过 500 个字符")
    private String applyReason;
}
