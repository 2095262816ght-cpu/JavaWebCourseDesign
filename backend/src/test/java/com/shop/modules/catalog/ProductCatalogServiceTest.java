package com.shop.modules.catalog;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.HomeShowcaseVO;
import com.shop.modules.catalog.dto.ProductCardVO;
import com.shop.modules.catalog.dto.ProductDetailVO;
import com.shop.modules.catalog.dto.ProductQueryCommand;
import com.shop.modules.catalog.service.ProductCatalogService;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductCatalogServiceTest {

    @Autowired
    private ProductCatalogService catalogService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("测试商城首页聚合橱窗获取与 Redis 缓存写入/命中")
    void testGetHomeShowcaseAndCache() {
        // 清理已有首页橱窗缓存
        redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);

        // 首次获取 (查库聚合 + 写入缓存)
        HomeShowcaseVO showcase = catalogService.getHomeShowcase();
        assertThat(showcase).isNotNull();
        assertThat(showcase.carousels()).isNotEmpty();
        assertThat(showcase.featuredBrands()).isNotEmpty();
        assertThat(showcase.categories()).isNotEmpty();
        assertThat(showcase.hotProducts()).isNotEmpty();
        assertThat(showcase.newProducts()).isNotEmpty();

        // 校验 Redis 缓存已被写入
        assertThat(redisTemplate.hasKey(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY)).isTrue();

        // 二次获取 (命中 Redis 缓存)
        HomeShowcaseVO cachedShowcase = catalogService.getHomeShowcase();
        assertThat(cachedShowcase).isNotNull();
        assertThat(cachedShowcase.hotProducts()).hasSameSizeAs(showcase.hotProducts());
    }

    @Test
    @DisplayName("测试商品多维条件组合检索")
    void testSearchProducts() {
        // 1. 关键字模糊搜索 "iPhone"
        ProductQueryCommand kwCmd = new ProductQueryCommand(
                "iPhone", null, null, null, null, null, 1, 10
        );
        PageResult<ProductCardVO> kwResult = catalogService.searchProducts(kwCmd);
        assertThat(kwResult.records()).isNotEmpty();
        assertThat(kwResult.records().get(0).name()).contains("iPhone");

        // 2. 品牌过滤: Apple (brandId = 1)
        ProductQueryCommand brandCmd = new ProductQueryCommand(
                null, null, 1L, null, null, null, 1, 10
        );
        PageResult<ProductCardVO> brandResult = catalogService.searchProducts(brandCmd);
        assertThat(brandResult.records()).isNotEmpty();
        brandResult.records().forEach(p -> assertThat(p.brandId()).isEqualTo(1L));

        // 3. 分类层级过滤: 顶级分类 "数码3C" (categoryId = 1) 包含其子分类商品
        ProductQueryCommand catCmd = new ProductQueryCommand(
                null, 1L, null, null, null, null, 1, 10
        );
        PageResult<ProductCardVO> catResult = catalogService.searchProducts(catCmd);
        assertThat(catResult.records()).isNotEmpty();

        // 4. 价格区间与排序过滤: 价格 1000 ~ 8000，价格升序
        ProductQueryCommand priceCmd = new ProductQueryCommand(
                null, null, null, new BigDecimal("1000"), new BigDecimal("8000"), "price_asc", 1, 10
        );
        PageResult<ProductCardVO> priceResult = catalogService.searchProducts(priceCmd);
        assertThat(priceResult.records()).isNotEmpty();
        if (priceResult.records().size() > 1) {
            assertThat(priceResult.records().get(0).price())
                    .isLessThanOrEqualTo(priceResult.records().get(1).price());
        }
    }

    @Test
    @DisplayName("测试商品详情获取与副图画廊解析")
    void testGetProductDetail() {
        // 清理缓存
        redisTemplate.delete(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + 1L);

        ProductDetailVO detail = catalogService.getProductDetail(1L);
        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.name()).contains("iPhone");
        assertThat(detail.brandName()).isEqualTo("Apple");
        assertThat(detail.categoryName()).isEqualTo("智能手机");
        assertThat(detail.subImages()).isNotEmpty();

        // 验证详情缓存已被写入
        assertThat(redisTemplate.hasKey(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + 1L)).isTrue();
    }
}
