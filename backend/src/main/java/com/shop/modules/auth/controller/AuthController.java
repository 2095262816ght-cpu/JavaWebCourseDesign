package com.shop.modules.auth.controller;

import com.shop.common.result.Result;
import com.shop.modules.auth.dto.AuthTokenVO;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import com.shop.modules.auth.dto.UpdateProfileCommand;
import com.shop.modules.auth.dto.UserProfileVO;
import com.shop.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心", description = "用户注册、统一登录、个人信息维护与会话注销")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "顾客账号注册", description = "公开接口：注册买家账号，密码由服务端自动完成安全加盐散列")
    @PostMapping("/register")
    public Result<UserProfileVO> register(@Valid @RequestBody RegisterCommand cmd) {
        UserProfileVO profile = authService.register(cmd);
        return Result.success("注册成功", profile);
    }

    @Operation(summary = "统一账号登录", description = "公开接口：支持顾客与管理员登录，返回 Sa-Token 令牌及用户信息")
    @PostMapping("/login")
    public Result<AuthTokenVO> login(@Valid @RequestBody LoginCommand cmd) {
        AuthTokenVO authToken = authService.login(cmd);
        return Result.success("登录成功", authToken);
    }

    @Operation(summary = "获取当前登录用户资料", description = "需登录：依据当前客户端携带的 Token 获取对应用户资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        UserProfileVO profile = authService.getCurrentUser();
        return Result.success(profile);
    }

    @Operation(summary = "更新当前登录用户资料", description = "需登录：支持更新昵称、头像以及验证原密码后修改新密码")
    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileCommand cmd) {
        UserProfileVO profile = authService.updateCurrentUserProfile(cmd);
        return Result.success("更新成功", profile);
    }

    @Operation(summary = "注销登录", description = "注销当前登录会话，失效对应 Token")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("注销成功", null);
    }
}
