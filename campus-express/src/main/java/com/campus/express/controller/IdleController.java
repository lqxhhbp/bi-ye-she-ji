package com.campus.express.controller;

import com.campus.express.common.Result;
import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.common.enums.Role;
import com.campus.express.dto.idle.IdleCalibrationRequest;
import com.campus.express.dto.idle.IdleReportRequest;
import com.campus.express.security.PublicEndpoint;
import com.campus.express.security.RequireRole;
import com.campus.express.service.IdleService;
import com.campus.express.vo.idle.CrowdSummaryVO;
import com.campus.express.vo.idle.IdleReportVO;
import com.campus.express.vo.idle.IdleTrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 空闲度接口
 */
@Tag(name = "M6 空闲度")
@RestController
@RequestMapping("/api/idle")
@RequiredArgsConstructor
public class IdleController {

    private final IdleService idleService;

    @RequireRole(Role.STUDENT)
    @Operation(summary = "学生上报空闲度")
    @PostMapping("/reports")
    public Result<Void> report(
            @Valid @RequestBody IdleReportRequest request,
            HttpServletRequest httpServletRequest
    ) {
        idleService.report(request, httpServletRequest.getRemoteAddr());
        return Result.success();
    }

    @PublicEndpoint
    @Operation(summary = "查询最近空闲度上报")
    @GetMapping("/reports/recent")
    public Result<List<IdleReportVO>> recentReports(@RequestParam Long stationId) {
        return Result.success(idleService.recent(stationId));
    }

    @PublicEndpoint
    @Operation(summary = "查询空闲度趋势")
    @GetMapping("/reports/trend")
    public Result<List<IdleTrendPointVO>> trend(
            @RequestParam Long stationId,
            @RequestParam(defaultValue = "1") int hours
    ) {
        return Result.success(idleService.trend(stationId, hours));
    }

    @RequireRole(Role.STATION_ADMIN)
    @OperationAudit(module = "空闲度", action = "站长校准")
    @Operation(summary = "站长校准空闲度")
    @PostMapping("/reports/calibrate")
    public Result<Void> calibrate(
            @Valid @RequestBody IdleCalibrationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        idleService.calibrate(request, httpServletRequest.getRemoteAddr());
        return Result.success();
    }

    @PublicEndpoint
    @Operation(summary = "查询空闲度概览")
    @GetMapping("/summary")
    public Result<List<CrowdSummaryVO>> summary() {
        return Result.success(idleService.summary());
    }
}
