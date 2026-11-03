package com.shop.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "修改个人资料命令")
public record UpdateProfileCommand(
        @Schema(description = "新显示昵称", example = "新昵称")
        @Size(max = 32, message = "昵称最多32位字符")
        String nickname,

        @Schema(description = "新头像URL", example = "/uploads/avatar.jpg")
        String avatar,

        @Schema(description = "原密码 (若需要修改密码时必填)")
        String oldPassword,

        @Schema(description = "新密码 (若需要修改密码时必填，6-32位)")
        @Size(min = 6, max = 32, message = "新密码长度须在6至32位之间")
        String newPassword
) {
}
