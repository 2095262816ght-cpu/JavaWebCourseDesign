package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "购物车行项展示数据")
public class CartItemVO {

    @Schema(description = "购物车记录ID", example = "10")
    private Long id;

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "iPhone 16 Pro Max")
    private String productName;

    @Schema(description = "商品主图URL")
    private String productImage;

    @Schema(description = "商品单价", example = "9999.00")
    private BigDecimal price;

    @Schema(description = "加购数量", example = "2")
    private Integer quantity;

    @Schema(description = "行项小计金额（单价 * 数量）", example = "19998.00")
    private BigDecimal subtotal;

    @Schema(description = "当前实时库存", example = "50")
    private Integer stock;

    @Schema(description = "是否勾选结算", example = "true")
    private Boolean checked;

    @Schema(description = "商品上下架状态（1:在售, 0:下架）", example = "1")
    private Integer productStatus;

    public CartItemVO() {
    }

    public CartItemVO(Long id, Long productId, String productName, String productImage,
                      BigDecimal price, Integer quantity, BigDecimal subtotal,
                      Integer stock, Boolean checked, Integer productStatus) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.stock = stock;
        this.checked = checked;
        this.productStatus = productStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public Integer getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(Integer productStatus) {
        this.productStatus = productStatus;
    }
}
