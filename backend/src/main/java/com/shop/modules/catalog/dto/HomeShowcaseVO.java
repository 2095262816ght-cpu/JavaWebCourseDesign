package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "商城首页聚合聚合展示视图对象 (消除瀑布流请求)")
public record HomeShowcaseVO(
        @Schema(description = "焦点轮播图列表")
        List<CarouselSlideVO> carousels,

        @Schema(description = "精选入驻品牌")
        List<BrandVO> featuredBrands,

        @Schema(description = "类目导航树")
        List<CategoryNodeVO> categories,

        @Schema(description = "热门推荐商品")
        List<ProductCardVO> hotProducts,

        @Schema(description = "最新上架商品")
        List<ProductCardVO> newProducts
) {}
