package com.shop.modules.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台商品新建与更新命令")
public record ProductCommand(
        @Schema(description = "商品ID (更新时必填，新建时为空)", example = "1")
        Long id,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称最长128个字符")
        @Schema(description = "商品名称", example = "iPhone 16 Pro Max 256GB")
        String name,

        @Size(max = 255, message = "副标题最长255个字符")
        @Schema(description = "副标题", example = "全新一代钛金属，超强性能")
        String subtitle,

        @NotNull(message = "所属品牌ID不能为空")
        @Schema(description = "品牌ID", example = "1")
        Long brandId,

        @NotNull(message = "所属分类ID不能为空")
        @Schema(description = "分类ID", example = "5")
        Long categoryId,

        @NotBlank(message = "商品主图地址不能为空")
        @Schema(description = "主图地址", example = "https://...")
        String mainImage,

        @Schema(description = "商品副图轮播图列表")
        List<String> subImages,

        @NotNull(message = "销售价格不能为空")
        @DecimalMin(value = "0.00", message = "价格不能小于0")
        @Schema(description = "销售价格", example = "9999.00")
        BigDecimal price,

        @Schema(description = "划线原价", example = "10999.00")
        BigDecimal originalPrice,

        @NotNull(message = "商品库存不能为空")
        @Min(value = 0, message = "库存不能小于0")
        @Schema(description = "库存", example = "50")
        Integer stock,

        @Min(value = 0, message = "状态值只能为 0 或 1")
        @Max(value = 1, message = "状态值只能为 0 或 1")
        @Schema(description = "上下架状态 (1: 上架, 0: 下架)", example = "1")
        Integer status,

        @Schema(description = "富文本详情 HTML")
        String detailHtml
) {
    public Integer statusOrDefault() {
        return status != null ? status : 1;
    }

    public BigDecimal originalPriceOrDefault() {
        return originalPrice != null ? originalPrice : price;
    }
}
