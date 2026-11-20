package com.shop.modules.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.service.CategoryService;
import com.shop.modules.catalog.service.impl.CategoryServiceImpl;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ShopCategoryMapper categoryMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanupCategoryCache() {
        redisTemplate.delete(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY);
        redisTemplate.delete(CategoryServiceImpl.ADMIN_CATEGORY_TREE_CACHE_KEY);
        redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
        var detailKeys = redisTemplate.keys(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + "*");
        if (detailKeys != null && !detailKeys.isEmpty()) {
            redisTemplate.delete(detailKeys);
        }
    }

    @Test
    @DisplayName("测试分类树查询、Cache-Aside 写入与命中")
    void testGetCategoryTreeAndCache() {
        // 先确保清除缓存
        redisTemplate.delete(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY);

        // 第一次调用：缓存未命中，回源 MySQL 查询并回写 Redis
        List<CategoryNodeVO> treeFirst = categoryService.getCategoryTree();
        assertThat(treeFirst).isNotEmpty();

        // 验证 Redis 中已存有缓存数据
        String cachedJson = redisTemplate.opsForValue().get(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY);
        assertThat(cachedJson).isNotBlank();

        // 第二次调用：直接命中 Redis 缓存
        List<CategoryNodeVO> treeSecond = categoryService.getCategoryTree();
        assertThat(treeSecond).hasSameSizeAs(treeFirst);
    }

    @Test
    @DisplayName("测试保存分类并触发缓存主动失效")
    void testSaveCategoryEvictsCache() {
        // 先触发缓存
        categoryService.getCategoryTree();
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY)).isTrue();

        // 新增一个测试子分类
        CategoryCommand cmd = new CategoryCommand(
                null,
                1L, // 手机数码
                "测试专区",
                "el-icon-star",
                99,
                1
        );
        categoryService.saveCategory(cmd);

        // 验证缓存已被主动清除
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY)).isFalse();

        // 重新获取分类树，应包含新增的测试专区
        List<CategoryNodeVO> refreshedTree = categoryService.getCategoryTree();
        CategoryNodeVO digital = refreshedTree.stream()
                .filter(node -> node.id().equals(1L))
                .findFirst()
                .orElseThrow();

        boolean containsNewCategory = digital.children().stream()
                .anyMatch(sub -> "测试专区".equals(sub.name()));
        assertThat(containsNewCategory).isTrue();
    }

    @Test
    @DisplayName("测试删除分类安全性：含有子分类时拒绝删除")
    void testDeleteCategoryWithChildrenFails() {
        // 手机数码 (id=1) 下存在智能手机和无线耳机等子分类
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子分类");
    }

    @Test
    @DisplayName("测试叶子分类正常删除并清除缓存")
    void testDeleteLeafCategorySuccess() {
        // 创建一个无子分类的独立叶子分类
        ShopCategory leaf = new ShopCategory(null, 0L, "临时分类", null, 100, 1);
        categoryMapper.insert(leaf);
        Long leafId = leaf.getId();

        // 预热缓存
        categoryService.getCategoryTree();
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY)).isTrue();

        // 执行删除
        categoryService.deleteCategory(leafId);

        // 验证缓存已被清除
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.CATEGORY_TREE_CACHE_KEY)).isFalse();
        assertThat(categoryMapper.selectById(leafId)).isNull();
    }

    @Test
    @DisplayName("测试禁止在同级分类下创建同名分类，不同父分类允许同名")
    void testSaveDuplicateSiblingCategoryFails() {
        // 顶级分类下已存在 "手机数码"
        CategoryCommand duplicateRoot = new CategoryCommand(
                null,
                0L,
                " 手机数码 ",
                null,
                10,
                1
        );
        assertThatThrownBy(() -> categoryService.saveCategory(duplicateRoot))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");

        // 不同父分类 (parentId=1 与 parentId=2) 下允许创建同名子分类
        categoryService.saveCategory(new CategoryCommand(
                null,
                1L,
                "周边配件",
                null,
                10,
                1
        ));
        categoryService.saveCategory(new CategoryCommand(
                null,
                2L,
                "周边配件",
                null,
                10,
                1
        ));
    }

    @Test
    @DisplayName("测试管理端分类树包含禁用节点、非法父级/循环父级校验与引用拦截")
    void testAdminCategoryTreeHierarchyGuardsAndDetailCacheEviction() {
        // 1. 预热商品详情缓存与管理端分类树缓存
        redisTemplate.opsForValue().set(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + 1L, "{\"mock\":1}");
        categoryService.getAdminCategoryTree();
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.ADMIN_CATEGORY_TREE_CACHE_KEY)).isTrue();

        // 2. 创建禁用状态根分类，验证前台树不返回但管理端树可正常查看
        categoryService.saveCategory(new CategoryCommand(null, 0L, "已禁用分类测试", null, 88, 0));
        assertThat(redisTemplate.hasKey(CategoryServiceImpl.ADMIN_CATEGORY_TREE_CACHE_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + 1L)).isFalse();

        List<CategoryNodeVO> portalTree = categoryService.getCategoryTree();
        List<CategoryNodeVO> adminTree = categoryService.getAdminCategoryTree();
        assertThat(portalTree.stream().anyMatch(n -> "已禁用分类测试".equals(n.name()))).isFalse();
        assertThat(adminTree.stream().anyMatch(n -> "已禁用分类测试".equals(n.name()) && n.status() == 0)).isTrue();

        // 3. 指定不存在的父级分类应抛出异常
        assertThatThrownBy(() -> categoryService.saveCategory(
                new CategoryCommand(null, 999999L, "孤儿分类", null, 1, 1)
        )).isInstanceOf(BusinessException.class).hasMessageContaining("父级分类不存在");

        // 4. 构建三层节点，禁止将祖先节点移动至其后代节点下形成循环
        ShopCategory root = new ShopCategory(null, 0L, "循环测试根", null, 1, 1);
        categoryMapper.insert(root);
        ShopCategory mid = new ShopCategory(null, root.getId(), "循环测试中", null, 1, 1);
        categoryMapper.insert(mid);
        ShopCategory leaf = new ShopCategory(null, mid.getId(), "循环测试叶", null, 1, 1);
        categoryMapper.insert(leaf);

        assertThatThrownBy(() -> categoryService.saveCategory(
                new CategoryCommand(root.getId(), leaf.getId(), "循环测试根", null, 1, 1)
        )).isInstanceOf(BusinessException.class).hasMessageContaining("循环层级");

        // 5. 仍有关联商品的分类 (id=5 智能手机) 不可删除
        assertThatThrownBy(() -> categoryService.deleteCategory(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关联");
    }
}
