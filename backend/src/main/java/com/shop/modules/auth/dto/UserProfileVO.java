package com.shop.modules.auth.dto;

import com.shop.modules.system.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户资料视图对象")
public record UserProfileVO(
        @Schema(description = "用户唯一ID", example = "1")
        Long id,

        @Schema(description = "登录用户名", example = "admin")
        String username,

        @Schema(description = "用户昵称", example = "平台超级管理员")
        String nickname,

        @Schema(description = "头像图片地址", example = "https://images.unsplash.com/...")
        String avatar,

        @Schema(description = "用户角色 (ROLE_ADMIN 或 ROLE_CUSTOMER)", example = "ROLE_ADMIN")
        String role,

        @Schema(description = "账号状态 (1: 正常, 0: 禁用)", example = "1")
        Integer status,

        @Schema(description = "账号注册时间")
        LocalDateTime createdAt
) {
    public static UserProfileVO from(SysUser user) {
        if (user == null) {
            return null;
        }
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
