package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "订单详情全量展示数据")
public class OrderDetailVO {

    @Schema(description = "订单主键ID", example = "101")
    private Long id;

    @Schema(description = "唯一业务订单编号", example = "ORD202611042130008899")
    private String orderNo;

    @Schema(description = "顾客ID", example = "2")
    private Long customerId;

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

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "订单包含的商品行项快照列表")
    private List<OrderItemVO> items = new ArrayList<>();

    public OrderDetailVO() {
    }

    public OrderDetailVO(Long id, String orderNo, Long customerId, BigDecimal totalAmount,
                         String status, DeliveryAddress deliveryAddress, LocalDateTime paymentTime,
                         LocalDateTime createdAt, LocalDateTime updatedAt, List<OrderItemVO> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        setDeliveryAddress(deliveryAddress);
        this.paymentTime = paymentTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items != null ? items : new ArrayList<>();
    }

    public OrderDetailVO(Long id, String orderNo, Long customerId, BigDecimal totalAmount,
                         String status, String receiverName, String receiverPhone,
                         String receiverAddress, LocalDateTime paymentTime,
                         LocalDateTime createdAt, LocalDateTime updatedAt, List<OrderItemVO> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.paymentTime = paymentTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getUserId() {
        return customerId;
    }

    public void setUserId(Long userId) {
        this.customerId = userId;
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

    /**
     * 获取封装的配送收件信息值对象
     */
    public DeliveryAddress getDeliveryAddress() {
        if (receiverName != null || receiverPhone != null || receiverAddress != null) {
            return new DeliveryAddress(receiverName, receiverPhone, receiverAddress);
        }
        return null;
    }

    public void setDeliveryAddress(DeliveryAddress deliveryAddress) {
        if (deliveryAddress != null) {
            this.receiverName = deliveryAddress.getReceiverName();
            this.receiverPhone = deliveryAddress.getReceiverPhone();
            this.receiverAddress = deliveryAddress.getReceiverAddress();
        }
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItemVO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVO> items) {
        this.items = items;
    }
}
