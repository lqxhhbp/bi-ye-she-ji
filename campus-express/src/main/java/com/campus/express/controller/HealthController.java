package com.campus.express.controller;

import com.campus.express.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 环境自检接口：验证 Spring Boot / MySQL 链路是否正常
 */
@Tag(name = "系统自检")
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name}")
    private String appName;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "健康检查", description = "返回服务状态、数据库连通性与版本信息")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("java", System.getProperty("java.version"));
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            data.put("database", jdbcTemplate.queryForObject("select version()", String.class));
            data.put("dbOk", true);
        } catch (Exception e) {
            data.put("dbOk", false);
            data.put("dbError", e.getMessage());
        }
        return Result.success(data);
    }
}
