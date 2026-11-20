package com.campus.express.dto.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 包裹核销请求
 */
@Data
public class VerifyPackageRequest {

    @NotBlank(message = "取件码不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{6,8}$", message = "取件码必须为 6-8 位数字或字母")
    private String pickupCode;
}
