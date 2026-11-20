package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "前台商品多维动态检索参数")
public record ProductQueryCommand(
        @Schema(description = "搜索关键字 (匹配商品标题与副标题)", example = "iPhone")
        String keyword,

        @Schema(description = "分类ID (支持父分类与叶子分类)", example = "1")
        Long categoryId,

        @Schema(description = "品牌ID", example = "1")
        Long brandId,

        @Schema(description = "最低价格", example = "1000")
        BigDecimal minPrice,

        @Schema(description = "最高价格", example = "10000")
        BigDecimal maxPrice,

        @Schema(description = "排序方式: price_asc(价格升序), price_desc(价格降序), newest(最新), 默认综合", example = "newest")
        String sortBy,

        @Schema(description = "当前页码", example = "1")
        Integer pageNum,

        @Schema(description = "每页大小", example = "12")
        Integer pageSize
) {
    public int pageNumOrDefault() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    public int pageSizeOrDefault() {
        if (pageSize == null || pageSize < 1) {
            return 12;
        }
        return Math.min(pageSize, 100);
    }
}
