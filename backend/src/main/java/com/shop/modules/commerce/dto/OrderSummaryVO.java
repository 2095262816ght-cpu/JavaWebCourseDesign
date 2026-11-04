package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "订单摘要列表展示数据")
public class OrderSummaryVO {

    @Schema(description = "订单主键ID", example = "101")
    private Long id;

    @Schema(description = "唯一业务订单编号", example = "ORD202611042130008899")
    private String orderNo;

    @Schema(description = "顾客用户ID", example = "2")
    private Long userId;

    @Schema(description = "订单总金额", example = "19998.00")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态（UNPAID, PAID, SHIPPED, COMPLETED, CANCELLED）", example = "UNPAID")
    private String status;

    @Schema(description = "收货人姓名", example = "张同学")
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800138000")
    private String receiverPhone;

    @Schema(description = "收货详细地址", example = "北京市海淀区中关村南大街1号")
    private String receiverAddress;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "下单创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "订单包含的商品总件数", example = "2")
    private Integer itemCount = 0;

    @Schema(description = "订单包含的商品行项快照")
    private List<OrderItemVO> items = new ArrayList<>();

    public OrderSummaryVO() {
    }

    public OrderSummaryVO(Long id, String orderNo, Long userId, BigDecimal totalAmount,
                          String status, String receiverName, String receiverPhone,
                          String receiverAddress, LocalDateTime paymentTime, LocalDateTime createdAt,
                          Integer itemCount, List<OrderItemVO> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.paymentTime = paymentTime;
        this.createdAt = createdAt;
        this.itemCount = itemCount != null ? itemCount : 0;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public List<OrderItemVO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVO> items) {
        this.items = items;
    }
}
