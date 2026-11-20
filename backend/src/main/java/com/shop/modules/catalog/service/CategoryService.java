package com.shop.modules.catalog.service;

import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;

import java.util.List;

public interface CategoryService {

    /**
     * 获取前台启用状态分类树（内置 Cache-Aside 缓存加速）
     *
     * @return 分类树节点列表
     */
    List<CategoryNodeVO> getCategoryTree();

    /**
     * 获取管理后台全量分类树（含启用与禁用状态分类，内置 Cache-Aside 缓存加速）
     *
     * @return 管理后台分类树节点列表
     */
    default List<CategoryNodeVO> getAdminCategoryTree() {
        return getCategoryTree();
    }

    /**
     * 新增或更新分类（执行后自动失效分类树缓存）
     *
     * @param cmd 分类参数
     */
    void saveCategory(CategoryCommand cmd);

    /**
     * 删除指定分类（若存在子分类则安全拦截，删除后自动失效分类树缓存）
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);
}
