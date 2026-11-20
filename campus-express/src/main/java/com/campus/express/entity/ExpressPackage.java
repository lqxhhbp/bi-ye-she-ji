package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 快递包裹
 */
@Data
@TableName("express_package")
public class ExpressPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String pickupCode;

    private String receiverPhone;

    private Long stationId;

    private String companyCode;

    private String shelfNo;

    private Integer status;

    private Long operatorId;

    private LocalDateTime inboundAt;

    private LocalDateTime takenAt;

    private String remark;
}
