package com.campus.express.vo.pkg;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Excel 导入错误行
 */
@Data
@AllArgsConstructor
public class ImportErrorVO {

    private Integer rowNumber;
    private String message;
}
