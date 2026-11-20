package com.campus.express.dto.station;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 站点代理快递公司请求
 */
@Data
public class StationCompanyRequest {

    @NotBlank(message = "快递公司编码不能为空")
    @Size(max = 32, message = "快递公司编码不能超过 32 个字符")
    private String companyCode;

    @NotBlank(message = "快递公司名称不能为空")
    @Size(max = 64, message = "快递公司名称不能超过 64 个字符")
    private String companyName;
}
