package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "更新购物车项入参命令")
public class UpdateCartItemCommand {

    @Schema(description = "修改后的数量（如传入则更新数量）", example = "2")
    @Min(value = 1, message = "商品数量至少为 1")
    private Integer quantity;

    @Schema(description = "勾选状态（true: 勾选, false: 取消勾选）", example = "true")
    private Boolean checked;

    public UpdateCartItemCommand() {
    }

    public UpdateCartItemCommand(Integer quantity, Boolean checked) {
        this.quantity = quantity;
        this.checked = checked;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }
}
