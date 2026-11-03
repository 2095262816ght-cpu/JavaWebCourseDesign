package com.shop.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "认证令牌响应视图")
public record AuthTokenVO(
        @Schema(description = "Token名称标识", example = "satoken")
        String tokenName,

        @Schema(description = "Token访问凭证串", example = "9b6426ec-27b0-4663-95ca-fb4c9a544b62")
        String tokenValue,

        @Schema(description = "已认证登录的用户信息")
        UserProfileVO user
) {
}
