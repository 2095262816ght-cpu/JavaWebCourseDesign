package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "订单结算创建成功结果")
public class OrderCheckoutResult {

    @Schema(description = "订单主键ID", example = "101")
    private Long orderId;

    @Schema(description = "唯一业务订单编号", example = "ORD202611042130008899")
    private String orderNo;

    @Schema(description = "应付总金额", example = "19998.00")
    private BigDecimal totalAmount;

    @Schema(description = "订单初始状态", example = "UNPAID")
    private String status;

    public OrderCheckoutResult() {
    }

    public OrderCheckoutResult(Long orderId, String orderNo, BigDecimal totalAmount, String status) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
