package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "后台管理商品检索条件")
public record AdminProductQuery(
        @Schema(description = "商品名称/关键字", example = "华为")
        String name,

        @Schema(description = "所属品牌ID", example = "2")
        Long brandId,

        @Schema(description = "所属分类ID", example = "5")
        Long categoryId,

        @Schema(description = "商品上下架状态 (1: 上架, 0: 下架)", example = "1")
        Integer status,

        @Schema(description = "当前页码", example = "1")
        Integer pageNum,

        @Schema(description = "每页大小", example = "10")
        Integer pageSize
) {
    public int pageNumOrDefault() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    public int pageSizeOrDefault() {
        return (pageSize == null || pageSize < 1 || pageSize > 100) ? 10 : pageSize;
    }
}
