package com.shop.modules.commerce.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.modules.commerce.dto.DeliveryAddress;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("shop_order")
public class ShopOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    @TableField("user_id")
    private Long customerId;

    private BigDecimal totalAmount;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField(exist = false)
    private DeliveryAddress deliveryAddress;

    private String status;

    private LocalDateTime paymentTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public ShopOrder() {
    }

    public ShopOrder(Long id, String orderNo, Long customerId, BigDecimal totalAmount,
                     DeliveryAddress deliveryAddress, String status, LocalDateTime paymentTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        setDeliveryAddress(deliveryAddress);
        this.status = status;
        this.paymentTime = paymentTime;
    }

    public ShopOrder(Long id, String orderNo, Long customerId, BigDecimal totalAmount,
                     String receiverName, String receiverPhone, String receiverAddress,
                     String status, LocalDateTime paymentTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryAddress = new DeliveryAddress(receiverName, receiverPhone, receiverAddress);
        this.status = status;
        this.paymentTime = paymentTime;
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

    public DeliveryAddress getDeliveryAddress() {
        if (deliveryAddress != null) {
            return deliveryAddress;
        }
        if (receiverName != null || receiverPhone != null || receiverAddress != null) {
            this.deliveryAddress = new DeliveryAddress(receiverName, receiverPhone, receiverAddress);
            return deliveryAddress;
        }
        return null;
    }

    public void setDeliveryAddress(DeliveryAddress deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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
        if (this.deliveryAddress != null) {
            this.deliveryAddress.setReceiverName(receiverName);
        }
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
        if (this.deliveryAddress != null) {
            this.deliveryAddress.setReceiverPhone(receiverPhone);
        }
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
        if (this.deliveryAddress != null) {
            this.deliveryAddress.setReceiverAddress(receiverAddress);
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
