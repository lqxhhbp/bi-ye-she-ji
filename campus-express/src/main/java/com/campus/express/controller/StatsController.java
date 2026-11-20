package com.campus.express.controller;

import com.campus.express.common.Result;
import com.campus.express.common.enums.Role;
import com.campus.express.security.RequireRole;
import com.campus.express.service.StatsService;
import com.campus.express.vo.stats.CrowdDistributionVO;
import com.campus.express.vo.stats.OperatorWorkloadVO;
import com.campus.express.vo.stats.StationDailyVO;
import com.campus.express.vo.stats.StationThroughputVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口
 */
@Tag(name = "M8 统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @RequireRole(Role.SUPER_ADMIN)
    @Operation(summary = "站点吞吐量排名")
    @GetMapping("/station-throughput")
    public Result<List<StationThroughputVO>> stationThroughput(
            @RequestParam(defaultValue = "7") int days
    ) {
        return Result.success(statsService.stationThroughput(days));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "本站每日统计")
    @GetMapping("/station-daily")
    public Result<List<StationDailyVO>> stationDaily(
            @RequestParam(defaultValue = "7") int days
    ) {
        return Result.success(statsService.stationDaily(days));
    }

    @RequireRole(Role.SUPER_ADMIN)
    @Operation(summary = "操作员工作量统计")
    @GetMapping("/operator-workload")
    public Result<List<OperatorWorkloadVO>> operatorWorkload(
            @RequestParam(defaultValue = "30") int days
    ) {
        return Result.success(statsService.operatorWorkload(days));
    }

    @RequireRole(Role.SUPER_ADMIN)
    @Operation(summary = "拥挤度分布")
    @GetMapping("/crowd-distribution")
    public Result<List<CrowdDistributionVO>> crowdDistribution() {
        return Result.success(statsService.crowdDistribution());
    }
}
