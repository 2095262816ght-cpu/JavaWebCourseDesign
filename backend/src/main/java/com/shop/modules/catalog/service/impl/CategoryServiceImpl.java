package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.cache.TransactionalCacheEvictor;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.CategoryService;
import com.shop.modules.catalog.service.util.CategoryTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    public static final String CATEGORY_TREE_CACHE_KEY = "catalog:category:tree";
    public static final String ADMIN_CATEGORY_TREE_CACHE_KEY = "catalog:category:admin:tree";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ShopCategoryMapper categoryMapper;
    private final ShopProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CategoryServiceImpl(ShopCategoryMapper categoryMapper,
                               ShopProductMapper productMapper,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CategoryNodeVO> getCategoryTree() {
        return loadCategoryTree(CATEGORY_TREE_CACHE_KEY, true);
    }

    @Override
    public List<CategoryNodeVO> getAdminCategoryTree() {
        return loadCategoryTree(ADMIN_CATEGORY_TREE_CACHE_KEY, false);
    }

    private List<CategoryNodeVO> loadCategoryTree(String cacheKey, boolean activeOnly) {
        // 1. 先查 Redis 缓存 (Cache-Aside 模式)
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, new TypeReference<List<CategoryNodeVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取分类树缓存异常，降级直查数据库: {}", e.getMessage());
        }

        // 2. 缓存未命中，回源 MySQL 查询分类
        LambdaQueryWrapper<ShopCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (activeOnly) {
            queryWrapper.eq(ShopCategory::getStatus, 1);
        }
        queryWrapper.orderByAsc(ShopCategory::getSort)
                .orderByAsc(ShopCategory::getId);
        List<ShopCategory> categories = categoryMapper.selectList(queryWrapper);

        // 3. 利用 Java 21 Stream 纯函数工具构建层级树
        List<CategoryNodeVO> tree = CategoryTreeBuilder.buildTree(categories);

        // 4. 回写 Redis 缓存并设置过期时间
        try {
            String treeJson = objectMapper.writeValueAsString(tree);
            redisTemplate.opsForValue().set(cacheKey, treeJson, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis 写入分类树缓存异常: {}", e.getMessage());
        }

        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategory(CategoryCommand cmd) {
        String categoryName = cmd.name() != null ? cmd.name().trim() : "";
        if (!StringUtils.hasText(categoryName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称不能为空");
        }
        Long parentId = cmd.parentIdOrDefault();
        if (parentId < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "父级分类ID不能为负数");
        }

        if (parentId > 0) {
            ShopCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "指定的父级分类不存在 (ID: " + parentId + ")");
            }
            if (cmd.id() != null) {
                if (cmd.id().equals(parentId)) {
                    throw new BusinessException("父级分类不能选择自身");
                }
                Set<Long> visited = new HashSet<>();
                Long ancestorId = parent.getParentId();
                while (ancestorId != null && ancestorId > 0 && visited.add(ancestorId)) {
                    if (cmd.id().equals(ancestorId)) {
                        throw new BusinessException("不可将分类移动至自身子分类下，避免形成循环层级");
                    }
                    ShopCategory ancestor = categoryMapper.selectById(ancestorId);
                    ancestorId = ancestor != null ? ancestor.getParentId() : null;
                }
            }
        }

        LambdaQueryWrapper<ShopCategory> dupWrapper = new LambdaQueryWrapper<ShopCategory>()
                .eq(ShopCategory::getParentId, parentId)
                .eq(ShopCategory::getName, categoryName);
        if (cmd.id() != null) {
            dupWrapper.ne(ShopCategory::getId, cmd.id());
        }
        Long dupCount = categoryMapper.selectCount(dupWrapper);
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "同级分类下已存在名称为 [" + categoryName + "] 的分类");
        }

        if (cmd.id() != null) {
            // 更新分类
            ShopCategory existing = categoryMapper.selectById(cmd.id());
            if (existing == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标分类不存在或已被删除");
            }

            existing.setName(categoryName);
            existing.setIcon(cmd.icon());
            existing.setParentId(parentId);
            existing.setSort(cmd.sortOrDefault());
            existing.setStatus(cmd.statusOrDefault());
            categoryMapper.updateById(existing);
        } else {
            // 新增分类
            ShopCategory category = new ShopCategory();
            category.setName(categoryName);
            category.setIcon(cmd.icon());
            category.setParentId(parentId);
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

        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<ShopProduct>().eq(ShopProduct::getCategoryId, id)
        );
        if (productCount != null && productCount > 0) {
            throw new BusinessException("该分类下仍关联 " + productCount + " 款商品，请先转移或删除相关商品");
        }

        categoryMapper.deleteById(id);

        // 数据发生变更，主动清除 Redis 缓存
        evictCategoryCache();
    }

    private void evictCategoryCache() {
        TransactionalCacheEvictor.evictNowAndAfterCommit(this::doEvictCategoryCache);
    }

    private void doEvictCategoryCache() {
        try {
            redisTemplate.delete(CATEGORY_TREE_CACHE_KEY);
            redisTemplate.delete(ADMIN_CATEGORY_TREE_CACHE_KEY);
            redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
            Set<String> detailKeys = redisTemplate.keys(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + "*");
            if (detailKeys != null && !detailKeys.isEmpty()) {
                redisTemplate.delete(detailKeys);
            }
        } catch (Exception e) {
            log.warn("主动清除分类树缓存失败: {}", e.getMessage());
        }
    }
}
