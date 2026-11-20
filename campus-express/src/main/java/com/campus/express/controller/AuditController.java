package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.Result;
import com.campus.express.common.annotation.OperationAudit;
import com.campus.express.common.enums.Role;
import com.campus.express.dto.audit.AuditReviewRequest;
import com.campus.express.security.RequireRole;
import com.campus.express.service.AuditService;
import com.campus.express.vo.audit.AuditVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站长审核接口
 */
@Tag(name = "M9 审核")
@RestController
@RequestMapping("/api/audits")
@RequireRole(Role.SUPER_ADMIN)
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "分页查询入驻审核")
    @GetMapping
    public Result<PageResult<AuditVO>> listAudits(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Integer status
    ) {
        return Result.success(auditService.list((int) page, (int) pageSize, status));
    }

    @OperationAudit(module = "审核", action = "审核站长申请")
    @Operation(summary = "审核站长入驻申请")
    @PutMapping("/{id}")
    public Result<Void> reviewAudit(
            @PathVariable Long id,
            @Valid @RequestBody AuditReviewRequest request
    ) {
        auditService.review(id, request);
        return Result.success();
    }
}
