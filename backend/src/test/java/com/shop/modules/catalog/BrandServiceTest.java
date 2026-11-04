package com.shop.modules.catalog;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.BrandCommand;
import com.shop.modules.catalog.dto.BrandQuery;
import com.shop.modules.catalog.dto.BrandVO;
import com.shop.modules.catalog.entity.ShopBrand;
import com.shop.modules.catalog.mapper.ShopBrandMapper;
import com.shop.modules.catalog.service.BrandService;
import com.shop.modules.catalog.service.impl.BrandServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BrandServiceTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private ShopBrandMapper brandMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("测试推荐品牌列表获取与 Cache-Aside 缓存")
    void testGetFeaturedBrands() {
        // 清理已有缓存
        Set<String> keys = redisTemplate.keys(BrandServiceImpl.BRAND_FEATURED_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        List<BrandVO> featured = brandService.getFeaturedBrands(4);
        assertThat(featured).isNotEmpty();
        assertThat(featured.size()).isLessThanOrEqualTo(4);

        // 验证缓存已被写入
        String cacheKey = BrandServiceImpl.BRAND_FEATURED_CACHE_PREFIX + 4;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 再次获取命中缓存
        List<BrandVO> cachedFeatured = brandService.getFeaturedBrands(4);
        assertThat(cachedFeatured).hasSameSizeAs(featured);
    }

    @Test
    @DisplayName("测试品牌多条件分页检索")
    void testListBrands() {
        // 1. 无条件第一页查询
        BrandQuery query = new BrandQuery(1, 5, null, null);
        PageResult<BrandVO> page1 = brandService.listBrands(query);
        assertThat(page1.records()).isNotEmpty();
        assertThat(page1.size()).isEqualTo(5);

        // 2. 关键词模糊匹配 (Apple)
        BrandQuery kwQuery = new BrandQuery(1, 10, "Apple", null);
        PageResult<BrandVO> appleResult = brandService.listBrands(kwQuery);
        assertThat(appleResult.records()).isNotEmpty();
        assertThat(appleResult.records().get(0).name()).contains("Apple");

        // 3. 状态过滤
        BrandQuery statusQuery = new BrandQuery(1, 10, null, 1);
        PageResult<BrandVO> activeResult = brandService.listBrands(statusQuery);
        assertThat(activeResult.records()).allMatch(b -> b.status() == 1);
    }

    @Test
    @DisplayName("测试品牌新增与更新并清除推荐缓存")
    void testSaveBrandEvictsCache() {
        // 预热缓存
        brandService.getFeaturedBrands(6);
        String cacheKey = BrandServiceImpl.BRAND_FEATURED_CACHE_PREFIX + 6;
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 新增品牌
        BrandCommand newBrand = new BrandCommand(
                null,
                "Tesla",
                "https://images.unsplash.com/photo-tesla",
                "加速全球向可持续能源的转变",
                10,
                1
        );
        brandService.saveBrand(newBrand);

        // 验证缓存已被清除
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }
}
