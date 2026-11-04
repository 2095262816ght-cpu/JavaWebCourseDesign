package com.shop.modules.catalog;

import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.service.util.CategoryTreeBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTreeBuilderTest {

    @Test
    @DisplayName("构建空分类列表返回空树")
    void testEmptyList() {
        List<CategoryNodeVO> result = CategoryTreeBuilder.buildTree(List.of());
        assertThat(result).isEmpty();

        List<CategoryNodeVO> nullResult = CategoryTreeBuilder.buildTree(null);
        assertThat(nullResult).isEmpty();
    }

    @Test
    @DisplayName("构建多级层级树结构与顺序正确")
    void testBuildMultiLevelTree() {
        ShopCategory root1 = new ShopCategory(1L, 0L, "手机数码", "icon-digital", 1, 1);
        ShopCategory root2 = new ShopCategory(2L, 0L, "电脑办公", "icon-pc", 2, 1);
        ShopCategory sub1 = new ShopCategory(3L, 1L, "智能手机", "icon-phone", 1, 1);
        ShopCategory sub2 = new ShopCategory(4L, 1L, "无线耳机", "icon-audio", 2, 1);
        ShopCategory subSub1 = new ShopCategory(5L, 3L, "5G折叠屏", "icon-fold", 1, 1);

        List<ShopCategory> flatList = List.of(sub2, root2, sub1, root1, subSub1);

        List<CategoryNodeVO> tree = CategoryTreeBuilder.buildTree(flatList);

        // 根节点应该有 2 个：手机数码、电脑办公
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).name()).isEqualTo("手机数码");
        assertThat(tree.get(1).name()).isEqualTo("电脑办公");

        // 手机数码下有 2 个子分类：智能手机、无线耳机
        List<CategoryNodeVO> children = tree.get(0).children();
        assertThat(children).hasSize(2);
        assertThat(children.get(0).name()).isEqualTo("智能手机");
        assertThat(children.get(1).name()).isEqualTo("无线耳机");

        // 智能手机下有 1 个三级子分类：5G折叠屏
        List<CategoryNodeVO> subChildren = children.get(0).children();
        assertThat(subChildren).hasSize(1);
        assertThat(subChildren.get(0).name()).isEqualTo("5G折叠屏");
        assertThat(subChildren.get(0).children()).isEmpty();
    }
}
