package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "购物车聚合摘要数据")
public class CartSummaryVO {

    @Schema(description = "购物车商品条目列表")
    private List<CartItemVO> items = new ArrayList<>();

    @Schema(description = "购物车中的商品款数", example = "3")
    private Integer totalItems = 0;

    @Schema(description = "已勾选的商品件数合计", example = "5")
    private Integer checkedCount = 0;

    @Schema(description = "勾选结算商品的总金额", example = "19998.00")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public CartSummaryVO() {
    }

    public CartSummaryVO(List<CartItemVO> items, Integer totalItems, Integer checkedCount, BigDecimal totalAmount) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalItems = totalItems != null ? totalItems : 0;
        this.checkedCount = checkedCount != null ? checkedCount : 0;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public List<CartItemVO> getItems() {
        return items;
    }

    public void setItems(List<CartItemVO> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getCheckedCount() {
        return checkedCount;
    }

    public void setCheckedCount(Integer checkedCount) {
        this.checkedCount = checkedCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
