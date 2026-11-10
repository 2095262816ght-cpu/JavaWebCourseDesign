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
    @DisplayName("测试品牌多条件分页检索及不同分页尺寸缓存隔离")
    void testListBrands() {
        // 1. 无条件第一页查询 (size=2)
        BrandQuery query2 = new BrandQuery(1, 2, null, null);
        PageResult<BrandVO> page2 = brandService.listBrands(query2);
        assertThat(page2.records()).hasSize(2);
        assertThat(page2.size()).isEqualTo(2);

        // 2. 无条件第一页查询 (size=4)，确保不与 size=2 产生缓存碰撞
        BrandQuery query4 = new BrandQuery(1, 4, null, null);
        PageResult<BrandVO> page4 = brandService.listBrands(query4);
        assertThat(page4.records()).hasSize(4);
        assertThat(page4.size()).isEqualTo(4);

        // 3. 关键词模糊匹配 (Apple)
        BrandQuery kwQuery = new BrandQuery(1, 10, "Apple", null);
        PageResult<BrandVO> appleResult = brandService.listBrands(kwQuery);
        assertThat(appleResult.records()).isNotEmpty();
        assertThat(appleResult.records().get(0).name()).contains("Apple");

        // 4. 状态过滤
        BrandQuery statusQuery = new BrandQuery(1, 10, null, 1);
        PageResult<BrandVO> activeResult = brandService.listBrands(statusQuery);
        assertThat(activeResult.records()).allMatch(b -> b.status() == 1);
    }

    @Test
    @DisplayName("测试品牌新增与更新并清除推荐与列表缓存")
    void testSaveBrandEvictsCache() {
        // 预热缓存
        brandService.getFeaturedBrands(6);
        brandService.listBrands(new BrandQuery(1, 10, null, null));
        String featuredKey = BrandServiceImpl.BRAND_FEATURED_CACHE_PREFIX + 6;
        String allKey = BrandServiceImpl.BRAND_ALL_CACHE_KEY;
        assertThat(redisTemplate.hasKey(featuredKey)).isTrue();
        assertThat(redisTemplate.hasKey(allKey)).isTrue();

        // 新增品牌（包含产地字段）
        BrandCommand newBrand = new BrandCommand(
                null,
                "Tesla",
                "美国德克萨斯",
                "https://images.unsplash.com/photo-tesla",
                "加速全球向可持续能源的转变",
                10,
                1
        );
        brandService.saveBrand(newBrand);

        // 验证产地保存正确
        PageResult<BrandVO> teslaResult = brandService.listBrands(new BrandQuery(1, 10, "Tesla", null));
        assertThat(teslaResult.records()).isNotEmpty();
        assertThat(teslaResult.records().get(0).origin()).isEqualTo("美国德克萨斯");

        // 验证缓存已被清除
        assertThat(redisTemplate.hasKey(featuredKey)).isFalse();
        assertThat(redisTemplate.hasKey(allKey)).isFalse();
    }
}
