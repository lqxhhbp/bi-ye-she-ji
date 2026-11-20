package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.Result;
import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.common.enums.Role;
import com.campus.express.entity.AlertRecord;
import com.campus.express.security.RequireRole;
import com.campus.express.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警接口
 */
@Tag(name = "M8 告警")
@RestController
@RequestMapping("/api/alerts")
@RequireRole(Role.SUPER_ADMIN)
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "分页查询告警")
    @GetMapping
    public Result<PageResult<AlertRecord>> listAlerts(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type
    ) {
        return Result.success(alertService.list((int) page, (int) pageSize, status, type));
    }

    @OperationAudit(module = "告警", action = "处理告警")
    @Operation(summary = "处理告警")
    @PutMapping("/{id}/handle")
    public Result<Void> handleAlert(@PathVariable Long id) {
        alertService.handle(id);
        return Result.success();
    }
}
