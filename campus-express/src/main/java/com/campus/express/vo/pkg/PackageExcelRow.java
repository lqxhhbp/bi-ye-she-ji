package com.campus.express.vo.pkg;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包裹导出行
 */
@Data
public class PackageExcelRow {

    @ExcelProperty("取件码")
    private String pickupCode;

    @ExcelProperty("收件人手机号")
    private String receiverPhone;

    @ExcelProperty("站点")
    private String stationName;

    @ExcelProperty("快递公司")
    private String companyName;

    @ExcelProperty("货架号")
    private String shelfNo;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("上架时间")
    private LocalDateTime inboundAt;

    @ExcelProperty("取件时间")
    private LocalDateTime takenAt;
}
