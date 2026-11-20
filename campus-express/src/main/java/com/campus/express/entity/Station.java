package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物理快递站点
 */
@Data
@TableName("station")
public class Station {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    private BigDecimal lng;

    private BigDecimal lat;

    private String businessHours;

    private String contactPhone;

    private Integer status;

    private Integer crowdLevel;

    private BigDecimal crowdScore;

    private LocalDateTime crowdUpdatedAt;

    private LocalDateTime createdAt;
}
