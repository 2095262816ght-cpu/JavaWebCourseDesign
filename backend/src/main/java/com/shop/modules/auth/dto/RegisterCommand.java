package com.shop.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "顾客新账号注册请求命令")
public record RegisterCommand(
        @Schema(description = "登录用户名（3-32位字母、数字或下划线）", example = "jasper_dev")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度须在3至32位之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅能包含英文字母、数字和下划线")
        String username,

        @Schema(description = "登录密码（6-32位字符）", example = "secret123")
        @NotBlank(message = "登录密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度须在6至32位之间")
        String password,

        @Schema(description = "用户显示昵称", example = "极客买家")
        @Size(max = 32, message = "昵称最多32位字符")
        String nickname,

        @Schema(description = "用户头像URL (可选)")
        String avatar
) {
}
