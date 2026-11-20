package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

        @Size(max = 64, message = "品牌产地最长64个字符")
        @Schema(description = "品牌产地/国家", example = "美国加利福尼亚")
        String origin,

        @Schema(description = "品牌Logo图片URL", example = "https://...")
        String logo,

        @Size(max = 512, message = "品牌描述最长512个字符")
        @Schema(description = "品牌描述", example = "全球领先科技公司")
        String description,

        @Schema(description = "排序序号 (越小越靠前)", example = "1")
        Integer sort,

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态值只能为 0 或 1")
        @Max(value = 1, message = "状态值只能为 0 或 1")
        @Schema(description = "状态 (1: 启用, 0: 禁用)", example = "1")
        Integer status
) {
    public BrandCommand(Long id, String name, String logo, String description, Integer sort, Integer status) {
        this(id, name, null, logo, description, sort, status);
    }
    public Integer sortOrDefault() {
        return sort != null ? sort : 0;
    }

    public Integer statusOrDefault() {
        return status != null ? status : 1;
    }
}
