package com.campus.express.dto.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 包裹上架请求
 */
@Data
public class PackageCreateRequest {

    @NotBlank(message = "取件码不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{6,8}$", message = "取件码必须为 6-8 位数字或字母")
    private String pickupCode;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "收件人手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "快递公司不能为空")
    private String companyCode;

    @NotBlank(message = "货架号不能为空")
    @Size(max = 32, message = "货架号不能超过 32 个字符")
    private String shelfNo;

    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String remark;
}
