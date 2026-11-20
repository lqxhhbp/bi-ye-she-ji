package com.campus.express.dto.pkg;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 包裹导入行
 */
@Data
public class PackageImportRow {

    @ExcelProperty("取件码")
    private String pickupCode;

    @ExcelProperty("收件人手机号")
    private String receiverPhone;

    @ExcelProperty("快递公司编码")
    private String companyCode;

    @ExcelProperty("货架号")
    private String shelfNo;

    @ExcelProperty("备注")
    private String remark;
}
