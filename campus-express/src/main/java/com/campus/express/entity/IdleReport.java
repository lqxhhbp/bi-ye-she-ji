package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空闲度上报
 */
@Data
@TableName("idle_report")
public class IdleReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private Long userId;

    private Integer level;

    private Integer source;

    private String ip;

    private LocalDateTime createdAt;
}
