package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.Result;
import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.common.enums.Role;
import com.campus.express.dto.station.StationCompanyRequest;
import com.campus.express.dto.station.StationSaveRequest;
import com.campus.express.security.PublicEndpoint;
import com.campus.express.security.RequireRole;
import com.campus.express.service.StationService;
import com.campus.express.vo.station.CompanyVO;
import com.campus.express.vo.station.StationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站点与品牌接口
 */
@Tag(name = "M2 站点与品牌")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @PublicEndpoint
    @Operation(summary = "分页查询站点")
    @GetMapping("/stations")
    public Result<PageResult<StationVO>> listStations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return Result.success(stationService.list((int) page, (int) pageSize));
    }

    @PublicEndpoint
    @Operation(summary = "查询站点详情")
    @GetMapping("/stations/{id}")
    public Result<StationVO> getStation(@PathVariable Long id) {
        return Result.success(stationService.get(id));
    }

    @RequireRole(Role.SUPER_ADMIN)
    @OperationAudit(module = "站点", action = "新增站点")
    @Operation(summary = "新增站点")
    @PostMapping("/stations")
    public Result<Void> createStation(@Valid @RequestBody StationSaveRequest request) {
        stationService.create(request);
        return Result.success();
    }

    @RequireRole({Role.SUPER_ADMIN, Role.STATION_ADMIN})
    @OperationAudit(module = "站点", action = "修改站点")
    @Operation(summary = "修改站点")
    @PutMapping("/stations/{id}")
    public Result<Void> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationSaveRequest request
    ) {
        stationService.update(id, request);
        return Result.success();
    }

    @RequireRole(Role.SUPER_ADMIN)
    @OperationAudit(module = "站点", action = "停用站点")
    @Operation(summary = "删除站点")
    @DeleteMapping("/stations/{id}")
    public Result<Void> deleteStation(@PathVariable Long id) {
        stationService.disable(id);
        return Result.success();
    }

    @PublicEndpoint
    @Operation(summary = "查询快递公司")
    @GetMapping("/companies")
    public Result<List<CompanyVO>> listCompanies() {
        return Result.success(stationService.listCompanies());
    }

    @RequireRole(Role.SUPER_ADMIN)
    @OperationAudit(module = "站点", action = "更新代理公司")
    @Operation(summary = "更新站点代理公司")
    @PutMapping("/stations/{id}/companies")
    public Result<Void> updateStationCompanies(
            @PathVariable Long id,
            @Valid @RequestBody List<StationCompanyRequest> companies
    ) {
        stationService.updateCompanies(id, companies);
        return Result.success();
    }
}
