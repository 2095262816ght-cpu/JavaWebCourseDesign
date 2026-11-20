package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "品牌列表分页筛选条件")
public record BrandQuery(
        @Schema(description = "当前页码", example = "1")
        Integer page,

        @Schema(description = "每页记录数", example = "10")
        Integer size,

        @Schema(description = "品牌名称或描述模糊搜索关键字", example = "Apple")
        String keyword,

        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status
) {
    public int getPageOrDefault() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getSizeOrDefault() {
        return (size == null || size < 1) ? 10 : Math.min(size, 100);
    }
}
