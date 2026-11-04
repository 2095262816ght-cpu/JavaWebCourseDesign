package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "商品卡片简略视图对象")
public record ProductCardVO(
        @Schema(description = "商品ID", example = "1")
        Long id,

        @Schema(description = "商品标题", example = "iPhone 16 Pro Max 256GB 原色钛金属")
        String name,

        @Schema(description = "商品副标题", example = "A18 Pro芯片，超长续航")
        String subtitle,

        @Schema(description = "主图URL", example = "https://images.unsplash.com/...")
        String mainImage,

        @Schema(description = "当前销售价", example = "9999.00")
        BigDecimal price,

        @Schema(description = "划线原价", example = "10999.00")
        BigDecimal originalPrice,

        @Schema(description = "品牌ID", example = "1")
        Long brandId,

        @Schema(description = "品牌名称", example = "Apple")
        String brandName,

        @Schema(description = "分类ID", example = "5")
        Long categoryId,

        @Schema(description = "分类名称", example = "智能手机")
        String categoryName,

        @Schema(description = "库存", example = "50")
        Integer stock,

        @Schema(description = "状态 (1: 上架, 0: 下架)", example = "1")
        Integer status
) {}
