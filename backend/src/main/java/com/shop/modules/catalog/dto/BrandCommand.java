package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "品牌保存与更新参数")
public record BrandCommand(
        @Schema(description = "品牌ID (修改时必填，新增时为空)", example = "1")
        Long id,

        @NotBlank(message = "品牌名称不能为空")
        @Size(max = 64, message = "品牌名称最长64个字符")
        @Schema(description = "品牌名称", example = "Apple")
        String name,

        @Schema(description = "品牌Logo图片URL", example = "https://...")
        String logo,

        @Size(max = 512, message = "品牌描述最长512个字符")
        @Schema(description = "品牌描述", example = "全球领先科技公司")
        String description,

        @Schema(description = "排序序号 (越小越靠前)", example = "1")
        Integer sort,

        @NotNull(message = "状态不能为空")
        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status
) {
    public Integer sortOrDefault() {
        return sort != null ? sort : 0;
    }

    public Integer statusOrDefault() {
        return status != null ? status : 1;
    }
}
