package com.shop.modules.catalog.service.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.modules.catalog.entity.ShopBrand;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.mapper.ShopBrandMapper;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 目录查询公共工具：分类层级展开与品牌/分类名称批量回填
 */
public final class CatalogLookupHelper {

    private CatalogLookupHelper() {
    }

    /**
     * 展开分类层级：返回指定分类自身及其全部后代分类 ID（逐层批量查询，避免递归 N+1）
     */
    public static List<Long> collectSelfAndDescendantIds(ShopCategoryMapper categoryMapper, Long rootCategoryId) {
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        visited.add(rootCategoryId);
        queue.add(rootCategoryId);

        while (!queue.isEmpty()) {
            List<Long> batchParents = new ArrayList<>();
            while (!queue.isEmpty()) {
                batchParents.add(queue.poll());
            }
            LambdaQueryWrapper<ShopCategory> childWrapper = new LambdaQueryWrapper<ShopCategory>()
                    .in(ShopCategory::getParentId, batchParents);
            List<ShopCategory> children = categoryMapper.selectList(childWrapper);
            if (children != null) {
                for (ShopCategory child : children) {
                    if (child.getId() != null && visited.add(child.getId())) {
                        queue.add(child.getId());
                    }
                }
            }
        }
        return new ArrayList<>(visited);
    }

    /**
     * 批量回填品牌 ID -> 品牌名称映射
     */
    public static Map<Long, String> loadBrandNames(ShopBrandMapper brandMapper, Collection<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return brandMapper.selectBatchIds(brandIds).stream()
                .collect(Collectors.toMap(ShopBrand::getId, ShopBrand::getName, (a, b) -> a));
    }

    /**
     * 批量回填分类 ID -> 分类名称映射
     */
    public static Map<Long, String> loadCategoryNames(ShopCategoryMapper categoryMapper, Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ShopCategory::getId, ShopCategory::getName, (a, b) -> a));
    }
}
