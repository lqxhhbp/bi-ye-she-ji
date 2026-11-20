package com.campus.express.controller;

import com.campus.express.common.Result;
import com.campus.express.common.enums.Role;
import com.campus.express.dto.auth.ChangePasswordRequest;
import com.campus.express.dto.auth.LoginRequest;
import com.campus.express.dto.auth.RegisterRequest;
import com.campus.express.dto.auth.StationApplyRequest;
import com.campus.express.security.PublicEndpoint;
import com.campus.express.security.RequireRole;
import com.campus.express.service.AuthService;
import com.campus.express.vo.auth.LoginVO;
import com.campus.express.vo.auth.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与用户接口
 */
@Tag(name = "M1 认证与权限")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PublicEndpoint
    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PublicEndpoint
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }

    @RequireRole(Role.STUDENT)
    @Operation(summary = "提交站长入驻申请")
    @PostMapping("/station/apply")
    public Result<Void> applyStation(@Valid @RequestBody StationApplyRequest request) {
        authService.applyStation(request);
        return Result.success();
    }
}
