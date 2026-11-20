package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "分类保存与更新参数")
public record CategoryCommand(
        @Schema(description = "分类ID (修改时必填，新增时为空)", example = "1")
        Long id,

        @Schema(description = "父级分类ID (0为根节点)", example = "0")
        Long parentId,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称最长64个字符")
        @Schema(description = "分类名称", example = "智能手机")
        String name,

        @Schema(description = "分类图标样式或图片地址", example = "el-icon-mobile")
        String icon,

        @Schema(description = "排序序号 (越小越靠前)", example = "1")
        Integer sort,

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值只能为 0 或 1")
        @Max(value = 1, message = "状态值只能为 0 或 1")
        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status
) {
    public Long parentIdOrDefault() {
        return parentId != null ? parentId : 0L;
    }

    public Integer sortOrDefault() {
        return sort != null ? sort : 0;
    }

    public Integer statusOrDefault() {
        return status != null ? status : 1;
    }
}
