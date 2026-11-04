package com.shop.modules.catalog.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.modules.catalog.dto.HomeShowcaseVO;
import com.shop.modules.catalog.dto.ProductCardVO;
import com.shop.modules.catalog.dto.ProductDetailVO;
import com.shop.modules.catalog.dto.ProductQueryCommand;
import com.shop.modules.catalog.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "前台商城-商品展示与检索", description = "面向公众顾客的首页聚合橱窗、多维动态商品搜索及商品详情")
@RestController
@RequestMapping("/api/portal")
public class PortalProductController {

    private final ProductCatalogService catalogService;

    public PortalProductController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "获取商城首页聚合橱窗数据", description = "单请求聚合轮播图、精选品牌、全量分类树、热卖与新品推荐，消除瀑布流请求")
    @GetMapping("/home")
    public Result<HomeShowcaseVO> getHomeShowcase() {
        HomeShowcaseVO showcase = catalogService.getHomeShowcase();
        return Result.success(showcase);
    }

    @Operation(summary = "多维动态条件检索商品", description = "支持关键字、品牌ID、分类ID（含子类目）、价格区间及多重排序")
    @GetMapping("/products")
    public Result<PageResult<ProductCardVO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize
    ) {
        ProductQueryCommand cmd = new ProductQueryCommand(
                keyword,
                categoryId,
                brandId,
                minPrice,
                maxPrice,
                sortBy,
                pageNum,
                pageSize
        );
        PageResult<ProductCardVO> result = catalogService.searchProducts(cmd);
        return Result.success(result);
    }

    @Operation(summary = "获取商品前台完整详情", description = "获取商品副图画廊、所属品牌和分类、当前库存等完整展示属性")
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> getProductDetail(@PathVariable Long id) {
        ProductDetailVO detail = catalogService.getProductDetail(id);
        return Result.success(detail);
    }
}
