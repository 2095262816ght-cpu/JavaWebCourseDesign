package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.service.CategoryService;
import com.shop.modules.catalog.service.util.CategoryTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    public static final String CATEGORY_TREE_CACHE_KEY = "catalog:category:tree";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ShopCategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CategoryServiceImpl(ShopCategoryMapper categoryMapper,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.categoryMapper = categoryMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CategoryNodeVO> getCategoryTree() {
        // 1. 先查 Redis 缓存 (Cache-Aside 模式)
        try {
            String cachedJson = redisTemplate.opsForValue().get(CATEGORY_TREE_CACHE_KEY);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, new TypeReference<List<CategoryNodeVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取分类树缓存异常，降级直查数据库: {}", e.getMessage());
        }

        // 2. 缓存未命中，回源 MySQL 查询启用状态的分类
        LambdaQueryWrapper<ShopCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCategory::getStatus, 1)
                .orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId);
        List<ShopCategory> categories = categoryMapper.selectList(queryWrapper);

        // 3. 利用 Java 21 Stream 纯函数工具构建层级树
        List<CategoryNodeVO> tree = CategoryTreeBuilder.buildTree(categories);

        // 4. 回写 Redis 缓存并设置过期时间
        try {
            String treeJson = objectMapper.writeValueAsString(tree);
            redisTemplate.opsForValue().set(CATEGORY_TREE_CACHE_KEY, treeJson, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis 写入分类树缓存异常: {}", e.getMessage());
        }

        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategory(CategoryCommand cmd) {
        if (cmd.id() != null) {
            // 更新分类
            ShopCategory existing = categoryMapper.selectById(cmd.id());
            if (existing == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标分类不存在或已被删除");
            }
            if (cmd.id().equals(cmd.parentId())) {
                throw new BusinessException("父级分类不能选择自身");
            }

            existing.setName(cmd.name());
            existing.setIcon(cmd.icon());
            existing.setParentId(cmd.parentIdOrDefault());
            existing.setSort(cmd.sortOrDefault());
            existing.setStatus(cmd.statusOrDefault());
            categoryMapper.updateById(existing);
        } else {
            // 新增分类
            ShopCategory category = new ShopCategory();
            category.setName(cmd.name());
            category.setIcon(cmd.icon());
            category.setParentId(cmd.parentIdOrDefault());
            category.setSort(cmd.sortOrDefault());
            category.setStatus(cmd.statusOrDefault());
            categoryMapper.insert(category);
        }

        // 数据发生变更，主动清除 Redis 缓存
        evictCategoryCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        ShopCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标分类不存在或已被删除");
        }

        // 级联安全检查：若当前分类下存在子分类，禁止直接删除
        Long childrenCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<ShopCategory>().eq(ShopCategory::getParentId, id)
        );
        if (childrenCount != null && childrenCount > 0) {
            throw new BusinessException("该分类下仍存在 " + childrenCount + " 个子分类，请先清理或转移子分类");
        }

        categoryMapper.deleteById(id);

        // 数据发生变更，主动清除 Redis 缓存
        evictCategoryCache();
    }

    private void evictCategoryCache() {
        try {
            redisTemplate.delete(CATEGORY_TREE_CACHE_KEY);
        } catch (Exception e) {
            log.warn("主动清除分类树缓存失败: {}", e.getMessage());
        }
    }
}
