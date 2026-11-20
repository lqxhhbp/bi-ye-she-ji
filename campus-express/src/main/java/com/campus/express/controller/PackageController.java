package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.Result;
import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.common.enums.Role;
import com.campus.express.dto.pkg.PackageCreateRequest;
import com.campus.express.dto.pkg.VerifyPackageRequest;
import com.campus.express.security.PublicEndpoint;
import com.campus.express.security.RequireRole;
import com.campus.express.service.PackageService;
import com.campus.express.vo.pkg.ImportResultVO;
import com.campus.express.vo.pkg.PackageTimelineVO;
import com.campus.express.vo.pkg.PackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 包裹上架、查件与核销接口
 */
@Tag(name = "M3/M4/M7 包裹业务")
@Validated
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @RequireRole(Role.STATION_ADMIN)
    @OperationAudit(module = "包裹", action = "上架包裹")
    @Operation(summary = "单个上架包裹")
    @PostMapping
    public Result<PackageVO> createPackage(@Valid @RequestBody PackageCreateRequest request) {
        return Result.success(packageService.create(request));
    }

    @RequireRole(Role.STATION_ADMIN)
    @OperationAudit(module = "包裹", action = "批量导入")
    @Operation(summary = "批量导入包裹")
    @PostMapping("/import")
    public Result<ImportResultVO> importPackages(@RequestParam("file") MultipartFile file) {
        return Result.success(packageService.importExcel(file));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "下载包裹导入模板")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=packages-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(packageService.importTemplate());
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "分页查询本站包裹")
    @GetMapping
    public Result<PageResult<PackageVO>> listPackages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(packageService.list((int) page, (int) pageSize, status, keyword));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "查询包裹详情")
    @GetMapping("/{id}")
    public Result<PackageVO> getPackage(@PathVariable Long id) {
        return Result.success(packageService.get(id));
    }

    @RequireRole(Role.STATION_ADMIN)
    @OperationAudit(module = "包裹", action = "撤销上架")
    @Operation(summary = "撤销上架")
    @PutMapping("/{id}/revoke")
    public Result<Void> revokePackage(@PathVariable Long id) {
        packageService.revoke(id);
        return Result.success();
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "今日包裹统计")
    @GetMapping("/stats/today")
    public Result<Map<String, Object>> todayStats() {
        return Result.success(new LinkedHashMap<>(packageService.todayStats()));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "导出包裹")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPackages() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=packages.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(packageService.exportExcel());
    }

    @PublicEndpoint
    @Operation(summary = "按手机号查件")
    @GetMapping("/query")
    public Result<List<PackageVO>> queryByPhone(
            @RequestParam
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            HttpServletRequest request
    ) {
        return Result.success(packageService.queryByPhone(phone, request.getRemoteAddr()));
    }

    @PublicEndpoint
    @Operation(summary = "按取件码查件")
    @GetMapping("/query/{pickupCode}")
    public Result<PackageVO> queryByPickupCode(
            @PathVariable String pickupCode,
            HttpServletRequest request
    ) {
        return Result.success(packageService.queryByPickupCode(pickupCode, request.getRemoteAddr()));
    }

    @RequireRole(Role.STUDENT)
    @Operation(summary = "查询包裹时间线")
    @GetMapping("/{id}/timeline")
    public Result<List<PackageTimelineVO>> packageTimeline(@PathVariable Long id) {
        return Result.success(packageService.timeline(id));
    }

    @RequireRole(Role.STUDENT)
    @Operation(summary = "分页查询本人包裹")
    @GetMapping("/mine")
    public Result<PageResult<PackageVO>> myPackages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Integer status
    ) {
        return Result.success(packageService.mine((int) page, (int) pageSize, status));
    }

    @RequireRole(Role.STATION_ADMIN)
    @OperationAudit(module = "包裹", action = "核销包裹")
    @Operation(summary = "核销包裹")
    @PostMapping("/verify")
    public Result<PackageVO> verifyPackage(
            @Valid @RequestBody VerifyPackageRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return Result.success(packageService.verify(request, httpServletRequest.getRemoteAddr()));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "按取件码查询待核销包裹")
    @GetMapping("/verify/{pickupCode}")
    public Result<PackageVO> getVerifiablePackage(@PathVariable String pickupCode) {
        return Result.success(packageService.getVerifiablePackage(pickupCode));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "查询已取包裹")
    @GetMapping("/taken")
    public Result<PageResult<PackageVO>> listTakenPackages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return Result.success(packageService.listTaken((int) page, (int) pageSize));
    }

    @RequireRole(Role.STATION_ADMIN)
    @Operation(summary = "查询超时未取包裹")
    @GetMapping("/overdue")
    public Result<PageResult<PackageVO>> listOverduePackages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return Result.success(packageService.listOverdue((int) page, (int) pageSize));
    }
}
