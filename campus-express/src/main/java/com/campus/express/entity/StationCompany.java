package com.campus.express.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 站点代理快递公司
 */
@Data
@TableName("station_company")
public class StationCompany {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stationId;

    private String companyCode;

    private String companyName;
}
