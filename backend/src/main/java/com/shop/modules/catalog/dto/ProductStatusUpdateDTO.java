package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "商品上下架状态更新请求")
public record ProductStatusUpdateDTO(
        @NotNull(message = "状态值不能为空 (1: 上架, 0: 下架)")
        @Schema(description = "状态值 (1: 上架, 0: 下架)", example = "1")
        Integer status
) {}
