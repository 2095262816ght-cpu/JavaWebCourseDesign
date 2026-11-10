package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "品牌详情视图对象")
public record BrandVO(
        @Schema(description = "品牌ID", example = "1")
        Long id,

        @Schema(description = "品牌名称", example = "Apple")
        String name,

        @Schema(description = "品牌产地/国家", example = "美国加利福尼亚")
        String origin,

        @Schema(description = "品牌Logo地址", example = "https://images.unsplash.com/...")
        String logo,

        @Schema(description = "品牌描述", example = "Apple 设计制造 iPhone 等数码产品")
        String description,

        @Schema(description = "排序序号 (越小越靠前)", example = "1")
        Integer sort,

        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status,

        @Schema(description = "创建时间")
        LocalDateTime createdAt,

        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
    public BrandVO(Long id, String name, String logo, String description, Integer sort, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, null, logo, description, sort, status, createdAt, updatedAt);
    }
}
