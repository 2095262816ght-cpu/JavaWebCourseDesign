package com.shop.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "统一登录请求命令")
public record LoginCommand(
        @Schema(description = "登录用户名", example = "admin")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "登录密码", example = "admin123")
        @NotBlank(message = "密码不能为空")
        String password
) {
}
