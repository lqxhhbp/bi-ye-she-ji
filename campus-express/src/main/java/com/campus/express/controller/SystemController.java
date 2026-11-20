package com.campus.express.controller;

import com.campus.express.common.PageResult;
import com.campus.express.common.Result;
import com.campus.express.common.enums.Role;
import com.campus.express.entity.OperationLog;
import com.campus.express.security.RequireRole;
import com.campus.express.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维与审计接口
 */
@Tag(name = "M9 运维")
@RestController
@RequestMapping("/api")
@RequireRole(Role.SUPER_ADMIN)
@RequiredArgsConstructor
public class SystemController {

    private final OperationLogService operationLogService;

    @Value("${spring.application.name}")
    private String appName;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> listLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action
    ) {
        return Result.success(operationLogService.list((int) page, (int) pageSize, module, action));
    }

    @Operation(summary = "查询系统信息")
    @GetMapping("/system/info")
    public Result<Map<String, Object>> systemInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("java", System.getProperty("java.version"));
        data.put("os", System.getProperty("os.name"));
        return Result.success(data);
    }
}
