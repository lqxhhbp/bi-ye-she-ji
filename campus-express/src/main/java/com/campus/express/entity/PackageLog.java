package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包裹状态流水
 */
@Data
@TableName("package_log")
public class PackageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packageId;

    private Integer action;

    private Long operatorId;

    private LocalDateTime createdAt;
}
