package com.campus.express.vo.pkg;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Excel 导入结果
 */
@Data
@Builder
public class ImportResultVO {

    private int total;
    private int success;
    private int failed;
    private List<ImportErrorVO> errors;
}
