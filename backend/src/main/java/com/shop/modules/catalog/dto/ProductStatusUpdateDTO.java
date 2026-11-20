package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "商品上下架状态更新请求")
public record ProductStatusUpdateDTO(
        @NotNull(message = "状态值不能为空 (1: 上架, 0: 下架)")
        @Min(value = 0, message = "非法状态值，只能为 0 或 1")
        @Max(value = 1, message = "非法状态值，只能为 0 或 1")
        @Schema(description = "状态值 (1: 上架, 0: 下架)", example = "1")
        Integer status
) {}
