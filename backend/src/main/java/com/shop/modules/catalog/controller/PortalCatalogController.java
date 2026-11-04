package com.shop.modules.catalog.controller;

import com.shop.common.result.Result;
import com.shop.modules.catalog.dto.BrandVO;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.service.BrandService;
import com.shop.modules.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台商城-商品目录与分类", description = "面向公众顾客的分类层级树展示与推荐品牌列表（Redis 缓存加速）")
@RestController
@RequestMapping("/api/portal")
public class PortalCatalogController {

    private final CategoryService categoryService;
    private final BrandService brandService;

    public PortalCatalogController(CategoryService categoryService, BrandService brandService) {
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @Operation(summary = "获取全量分类层级树", description = "前台分类导航：采用 Cache-Aside Redis 缓存，层级递归装配")
    @GetMapping("/categories/tree")
    public Result<List<CategoryNodeVO>> getCategoryTree() {
        List<CategoryNodeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取热门推荐品牌", description = "前台首页品牌展示：支持自定义限制条数，默认前 6 个热门品牌")
    @GetMapping("/brands/featured")
    public Result<List<BrandVO>> getFeaturedBrands(
            @RequestParam(defaultValue = "6") int limit
    ) {
        List<BrandVO> brands = brandService.getFeaturedBrands(limit);
        return Result.success(brands);
    }
}
