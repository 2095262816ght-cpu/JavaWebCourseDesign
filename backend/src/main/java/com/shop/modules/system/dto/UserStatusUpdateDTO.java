package com.shop.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "修改用户状态数据传输对象")
public record UserStatusUpdateDTO(
        @Schema(description = "账号状态 (1: 启用, 0: 禁用)", example = "0")
        @NotNull(message = "状态值不能为空")
        @Min(value = 0, message = "非法状态值，只能为 0 或 1")
        @Max(value = 1, message = "非法状态值，只能为 0 或 1")
        Integer status
) {
}
