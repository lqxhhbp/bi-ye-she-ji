package com.campus.express.vo.pkg;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 包裹展示信息
 */
@Data
@Builder
public class PackageVO {

    private Long id;
    private String pickupCode;
    private String receiverPhone;
    private Long stationId;
    private String stationName;
    private String companyCode;
    private String companyName;
    private String shelfNo;
    private Integer status;
    private LocalDateTime inboundAt;
    private LocalDateTime takenAt;
    private String remark;
}
