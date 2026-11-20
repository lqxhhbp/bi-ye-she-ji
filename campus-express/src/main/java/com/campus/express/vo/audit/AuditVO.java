package com.campus.express.vo.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站长入驻审核展示信息
 */
@Data
@Builder
public class AuditVO {

    private Long id;
    private Long userId;
    private String applicantName;
    private String applicantPhone;
    private Long stationId;
    private String stationName;
    private String applyReason;
    private Integer status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createdAt;
}
