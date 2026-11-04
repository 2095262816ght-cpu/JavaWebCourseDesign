package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "订单商品行项快照展示数据")
public class OrderItemVO {

    @Schema(description = "订单项ID", example = "501")
    private Long id;

    @Schema(description = "订单ID", example = "101")
    private Long orderId;

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "下单时商品名称快照", example = "iPhone 16 Pro Max")
    private String productName;

    @Schema(description = "下单时商品主图快照")
    private String productImage;

    @Schema(description = "购买单价快照", example = "9999.00")
    private BigDecimal unitPrice;

    @Schema(description = "购买件数", example = "2")
    private Integer quantity;

    @Schema(description = "行项金额小计", example = "19998.00")
    private BigDecimal totalPrice;

    public OrderItemVO() {
    }

    public OrderItemVO(Long id, Long orderId, Long productId, String productName,
                       String productImage, BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
