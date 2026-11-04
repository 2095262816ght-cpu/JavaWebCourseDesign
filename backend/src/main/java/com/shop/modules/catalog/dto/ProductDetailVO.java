package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "商品详情完整视图对象")
public record ProductDetailVO(
        @Schema(description = "商品ID", example = "1")
        Long id,

        @Schema(description = "品牌ID", example = "1")
        Long brandId,

        @Schema(description = "品牌名称", example = "Apple")
        String brandName,

        @Schema(description = "品牌Logo", example = "https://...")
        String brandLogo,

        @Schema(description = "分类ID", example = "5")
        Long categoryId,

        @Schema(description = "分类名称", example = "智能手机")
        String categoryName,

        @Schema(description = "商品名称", example = "iPhone 16 Pro Max 256GB 原色钛金属")
        String name,

        @Schema(description = "商品副标题", example = "A18 Pro芯片，超长续航，4800万像素超广角摄像头")
        String subtitle,

        @Schema(description = "主图URL", example = "https://...")
        String mainImage,

        @Schema(description = "详情副图轮播画廊列表")
        List<String> subImages,

        @Schema(description = "销售价格", example = "9999.00")
        BigDecimal price,

        @Schema(description = "划线原价", example = "10999.00")
        BigDecimal originalPrice,

        @Schema(description = "库存", example = "50")
        Integer stock,

        @Schema(description = "状态 (1: 上架, 0: 下架)", example = "1")
        Integer status,

        @Schema(description = "富文本详情 HTML 内容")
        String detailHtml,

        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {}
