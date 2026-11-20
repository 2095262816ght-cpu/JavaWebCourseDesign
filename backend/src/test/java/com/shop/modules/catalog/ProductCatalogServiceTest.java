package com.shop.modules.catalog;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.HomeShowcaseVO;
import com.shop.modules.catalog.dto.ProductCardVO;
import com.shop.modules.catalog.dto.ProductDetailVO;
import com.shop.modules.catalog.dto.ProductQueryCommand;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.ProductCatalogService;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductCatalogServiceTest {

    @Autowired
    private ProductCatalogService catalogService;

    @Autowired
    private ShopCategoryMapper categoryMapper;

    @Autowired
    private ShopProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanupCatalogRedisCache() {
        Set<String> detailKeys = redisTemplate.keys(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + "*");
        if (detailKeys != null && !detailKeys.isEmpty()) {
            redisTemplate.delete(detailKeys);
        }
        redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
    }

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
        assertThat(detail.brandOrigin()).isEqualTo("美国加利福尼亚");
        assertThat(detail.categoryName()).isEqualTo("智能手机");
        assertThat(detail.subImages()).isNotEmpty();

        // 验证详情缓存已被写入
        assertThat(redisTemplate.hasKey(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + 1L)).isTrue();
    }

    @Test
    @DisplayName("测试三级及以上分类层次结构商品递归过滤")
    void testSearchProductsAcrossDeepCategoryHierarchy() {
        ShopCategory root = new ShopCategory(null, 0L, "深层测试一级", null, 99, 1);
        categoryMapper.insert(root);
        ShopCategory level2 = new ShopCategory(null, root.getId(), "深层测试二级", null, 1, 1);
        categoryMapper.insert(level2);
        ShopCategory level3 = new ShopCategory(null, level2.getId(), "深层测试三级", null, 1, 1);
        categoryMapper.insert(level3);

        ShopProduct deepProduct = new ShopProduct();
        deepProduct.setBrandId(1L);
        deepProduct.setCategoryId(level3.getId());
        deepProduct.setName("三级分类专属测试设备");
        deepProduct.setSubtitle("测试深层递归分类查询");
        deepProduct.setMainImage("https://img.test/deep.jpg");
        deepProduct.setPrice(new BigDecimal("199.00"));
        deepProduct.setOriginalPrice(new BigDecimal("299.00"));
        deepProduct.setStock(10);
        deepProduct.setStatus(1);
        productMapper.insert(deepProduct);

        PageResult<ProductCardVO> result = catalogService.searchProducts(
                new ProductQueryCommand(null, root.getId(), null, null, null, null, 1, 10)
        );
        assertThat(result.records())
                .extracting(ProductCardVO::id)
                .contains(deepProduct.getId());
    }
}
