package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "添加商品至购物车入参命令")
public class AddCartCommand {

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "购买数量（默认1）", example = "1")
    @Min(value = 1, message = "添加数量至少为 1")
    private Integer quantity = 1;

    public AddCartCommand() {
    }

    public AddCartCommand(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity != null ? quantity : 1;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
