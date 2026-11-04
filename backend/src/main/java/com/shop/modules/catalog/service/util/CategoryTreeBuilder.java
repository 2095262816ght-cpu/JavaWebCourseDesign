package com.shop.modules.catalog.service.util;

import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 纯函数式分类层级树构建工具
 * 利用 Java 21 Stream API 分组与递归映射，保持无状态与高内聚，便于单测与并发安全
 */
public final class CategoryTreeBuilder {

    private static final Comparator<ShopCategory> SORT_COMPARATOR = Comparator
            .comparing((ShopCategory c) -> c.getSort() == null ? 0 : c.getSort())
            .thenComparing(c -> c.getId() == null ? 0L : c.getId());

    private CategoryTreeBuilder() {
    }

    /**
     * 将扁平的分类列表构建为多级树形结构
     *
     * @param categories 扁平分类实体列表
     * @return 嵌套的根级分类节点树列表
     */
    public static List<CategoryNodeVO> buildTree(List<ShopCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        // 按 parentId 分组子分类
        Map<Long, List<ShopCategory>> parentMap = categories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .sorted(SORT_COMPARATOR)
                .collect(Collectors.groupingBy(ShopCategory::getParentId));

        // 筛选根节点 (parentId 为 0 或 null) 并递归装配
        return categories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0L)
                .sorted(SORT_COMPARATOR)
                .map(root -> toNodeVO(root, parentMap))
                .toList();
    }

    private static CategoryNodeVO toNodeVO(ShopCategory current, Map<Long, List<ShopCategory>> parentMap) {
        List<ShopCategory> childrenEntities = parentMap.getOrDefault(current.getId(), List.of());
        List<CategoryNodeVO> childrenNodes = childrenEntities.stream()
                .sorted(SORT_COMPARATOR)
                .map(child -> toNodeVO(child, parentMap))
                .toList();

        return CategoryNodeVO.of(
                current.getId(),
                current.getParentId() == null ? 0L : current.getParentId(),
                current.getName(),
                current.getIcon(),
                current.getSort() == null ? 0 : current.getSort(),
                current.getStatus() == null ? 1 : current.getStatus(),
                childrenNodes
        );
    }
}
