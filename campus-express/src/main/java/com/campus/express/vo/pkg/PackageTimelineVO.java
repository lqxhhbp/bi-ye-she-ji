package com.campus.express.vo.pkg;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包裹状态时间线
 */
@Data
@AllArgsConstructor
public class PackageTimelineVO {

    private Integer action;
    private String actionName;
    private LocalDateTime createdAt;
}
