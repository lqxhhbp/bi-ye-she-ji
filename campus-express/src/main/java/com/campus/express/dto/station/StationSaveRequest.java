package com.campus.express.dto.station;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 站点新增或修改请求
 */
@Data
public class StationSaveRequest {

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 100, message = "站点名称不能超过 100 个字符")
    private String name;

    @NotBlank(message = "站点地址不能为空")
    @Size(max = 255, message = "站点地址不能超过 255 个字符")
    private String address;

    @DecimalMin(value = "-180", message = "经度不在合法范围内")
    @DecimalMax(value = "180", message = "经度不在合法范围内")
    private BigDecimal lng;

    @DecimalMin(value = "-90", message = "纬度不在合法范围内")
    @DecimalMax(value = "90", message = "纬度不在合法范围内")
    private BigDecimal lat;

    @NotBlank(message = "营业时间不能为空")
    @Size(max = 100, message = "营业时间不能超过 100 个字符")
    private String businessHours;

    @Pattern(regexp = "^$|^[0-9+\\-]{5,20}$", message = "联系电话格式不正确")
    private String contactPhone;

    private Integer status;
}
