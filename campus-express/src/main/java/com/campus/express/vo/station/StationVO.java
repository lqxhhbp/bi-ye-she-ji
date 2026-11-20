package com.campus.express.vo.station;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 站点详情
 */
@Data
@Builder
public class StationVO {

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
    private List<CompanyVO> companies;
}
