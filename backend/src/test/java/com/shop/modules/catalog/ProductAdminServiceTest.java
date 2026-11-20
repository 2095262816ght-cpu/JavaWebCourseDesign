package com.shop.modules.catalog;

import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.AdminProductQuery;
import com.shop.modules.catalog.dto.ProductAdminVO;
import com.shop.modules.catalog.dto.ProductCommand;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.ProductAdminService;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductAdminServiceTest {

    @Autowired
    private ProductAdminService productAdminService;

    @Autowired
    private ShopProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("测试新建商品及缓存主动失效")
    void testSaveProductAndEvictCache() {
        // 预设缓存
        redisTemplate.opsForValue().set(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY, "dummy_showcase");

        ProductCommand cmd = new ProductCommand(
                null,
                "测试旗舰手机 Neo 1",
                "极速超薄 强劲续航",
                1L, // Apple
                5L, // 智能手机
                "https://images.unsplash.com/main.png",
                List.of("https://images.unsplash.com/sub1.png", "https://images.unsplash.com/sub2.png"),
                new BigDecimal("4999.00"),
                new BigDecimal("5499.00"),
                100,
                1,
                "<p>测试详情</p>"
        );

        Long newId = productAdminService.saveProduct(cmd);
        assertThat(newId).isNotNull().isPositive();

        // 验证数据库插入成功
        ShopProduct inserted = productMapper.selectById(newId);
        assertThat(inserted).isNotNull();
        assertThat(inserted.getName()).isEqualTo("测试旗舰手机 Neo 1");
        assertThat(inserted.getPrice()).isEqualByComparingTo("4999.00");
        assertThat(inserted.getSubImages()).contains("sub1.png,https://images.unsplash.com/sub2.png");

        // 验证首页橱窗缓存已被主动淘汰
        assertThat(redisTemplate.hasKey(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY)).isFalse();
    }

    @Test
    @DisplayName("测试关联不存在的品牌或分类时抛出校验异常")
    void testSaveProductValidationFailure() {
        // 不存在的 brandId = 999999
        ProductCommand invalidBrandCmd = new ProductCommand(
                null, "非法品牌商品", null, 999999L, 5L,
                "https://img.png", null, new BigDecimal("99.00"), null, 10, 1, null
        );
        assertThatThrownBy(() -> productAdminService.saveProduct(invalidBrandCmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("品牌不存在");

        // 不存在的 categoryId = 999999
        ProductCommand invalidCatCmd = new ProductCommand(
                null, "非法分类商品", null, 1L, 999999L,
                "https://img.png", null, new BigDecimal("99.00"), null, 10, 1, null
        );
        assertThatThrownBy(() -> productAdminService.saveProduct(invalidCatCmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("测试快速变更商品上下架状态")
    void testUpdateStatus() {
        productAdminService.updateStatus(1L, 0);
        ShopProduct p1 = productMapper.selectById(1L);
        assertThat(p1.getStatus()).isEqualTo(0);

        productAdminService.updateStatus(1L, 1);
        ShopProduct p2 = productMapper.selectById(1L);
        assertThat(p2.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试管理后台多条件分页列表检索")
    void testListAdminProducts() {
        AdminProductQuery query = new AdminProductQuery(
                "iPhone", 1L, null, 1, 1, 10
        );
        PageResult<ProductAdminVO> result = productAdminService.listAdminProducts(query);
        assertThat(result.total()).isGreaterThanOrEqualTo(1);
        assertThat(result.records().get(0).brandName()).isEqualTo("Apple");
        assertThat(result.records().get(0).categoryName()).isEqualTo("智能手机");
    }

    @Test
    @DisplayName("测试管理后台按父分类筛选可命中子分类下商品")
    void testListAdminProductsWithParentCategoryIncludesChildren() {
        AdminProductQuery parentCategoryQuery = new AdminProductQuery(
                null, null, 1L, null, 1, 20
        );
        PageResult<ProductAdminVO> result = productAdminService.listAdminProducts(parentCategoryQuery);
        assertThat(result.total()).isGreaterThanOrEqualTo(1);
        assertThat(result.records())
                .extracting(ProductAdminVO::categoryId)
                .anyMatch(cid -> cid != null && !cid.equals(1L));

        PageResult<ProductAdminVO> defaultResult = productAdminService.listAdminProducts(null);
        assertThat(defaultResult.current()).isEqualTo(1L);
    }

    @Test
    @DisplayName("测试商品删除")
    void testDeleteProduct() {
        ProductCommand cmd = new ProductCommand(
                null, "待删除测试商品", null, 1L, 5L,
                "https://img.png", null, new BigDecimal("199.00"), null, 5, 1, null
        );
        Long id = productAdminService.saveProduct(cmd);
        assertThat(id).isNotNull();

        productAdminService.deleteProduct(id);
        ShopProduct deleted = productMapper.selectById(id);
        assertThat(deleted).isNull(); // @TableLogic 逻辑删除后 selectById 返回 null
    }
}
