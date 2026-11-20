package com.campus.express.controller;

import com.campus.express.common.Result;
import com.campus.express.common.enums.Role;
import com.campus.express.entity.Station;
import com.campus.express.security.PublicEndpoint;
import com.campus.express.security.RequireRole;
import com.campus.express.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 地图接口
 */
@Tag(name = "M5 地图")
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final StationService stationService;

    @PublicEndpoint
    @Operation(summary = "查询地图站点")
    @GetMapping("/stations")
    public Result<List<Station>> mapStations() {
        return Result.success(stationService.listMapStations());
    }

    @PublicEndpoint
    @Operation(summary = "规划路线")
    @GetMapping("/route")
    public Result<Map<String, Object>> route(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return Result.success(Map.of());
    }

    @PublicEndpoint
    @Operation(summary = "查询附近站点")
    @GetMapping("/nearby")
    public Result<List<Station>> nearby(
            @RequestParam Double lng,
            @RequestParam Double lat
    ) {
        return Result.success(stationService.nearby(lng, lat));
    }

    @RequireRole(Role.SUPER_ADMIN)
    @Operation(summary = "地址地理编码")
    @PostMapping("/geocode")
    public Result<Map<String, Object>> geocode(@RequestBody Map<String, Object> request) {
        return Result.success(Map.of());
    }
}
