package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分类树节点视图对象")
public record CategoryNodeVO(
        @Schema(description = "分类ID", example = "1")
        Long id,

        @Schema(description = "父级分类ID (0为根分类)", example = "0")
        Long parentId,

        @Schema(description = "分类名称", example = "手机数码")
        String name,

        @Schema(description = "分类图标", example = "el-icon-phone")
        String icon,

        @Schema(description = "排序序号 (越小越靠前)", example = "1")
        Integer sort,

        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status,

        @Schema(description = "子分类列表")
        List<CategoryNodeVO> children
) {
    public static CategoryNodeVO of(Long id, Long parentId, String name, String icon, Integer sort, Integer status, List<CategoryNodeVO> children) {
        return new CategoryNodeVO(id, parentId, name, icon, sort, status, children != null ? children : List.of());
    }
}
