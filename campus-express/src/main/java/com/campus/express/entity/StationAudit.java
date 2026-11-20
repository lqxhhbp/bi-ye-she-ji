package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站长入驻审核
 */
@Data
@TableName("station_audit")
public class StationAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long stationId;

    private String applyReason;

    private Integer status;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private LocalDateTime createdAt;
}
