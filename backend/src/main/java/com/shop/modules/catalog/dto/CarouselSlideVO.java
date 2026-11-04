package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "商城首页轮播图对象")
public record CarouselSlideVO(
        @Schema(description = "轮播项ID", example = "1")
        Long id,

        @Schema(description = "主标题", example = "iPhone 16 Pro 钛金属系列")
        String title,

        @Schema(description = "副标题/广告语", example = "强悍 A18 Pro 芯片，超长续航与卓越影像")
        String subtitle,

        @Schema(description = "图片URL", example = "https://images.unsplash.com/...")
        String image,

        @Schema(description = "跳转目标链接", example = "/product/1")
        String targetUrl,

        @Schema(description = "关联商品ID (可选)", example = "1")
        Long productId
) {}
