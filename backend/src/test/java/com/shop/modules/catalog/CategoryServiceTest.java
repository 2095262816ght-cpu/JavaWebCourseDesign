package com.shop.modules.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.service.CategoryService;
import com.shop.modules.catalog.service.impl.CategoryServiceImpl;
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
}
