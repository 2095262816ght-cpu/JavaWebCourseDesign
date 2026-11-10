package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

@Schema(description = "配送收货人与地址信息")
public class DeliveryAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收货人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张同学")
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 64, message = "收货人姓名长度不能超过 64 个字符")
    private String receiverName;

    @Schema(description = "收货人联系手机", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "收货人联系电话不能为空")
    @Size(max = 32, message = "联系电话长度不能超过 32 个字符")
    private String receiverPhone;

    @Schema(description = "详细收货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市海淀区中关村南大街1号")
    @NotBlank(message = "详细收货地址不能为空")
    @Size(max = 255, message = "收货地址长度不能超过 255 个字符")
    private String receiverAddress;

    public DeliveryAddress() {
    }

    public DeliveryAddress(String receiverName, String receiverPhone, String receiverAddress) {
        this.receiverName = receiverName != null ? receiverName.trim() : null;
        this.receiverPhone = receiverPhone != null ? receiverPhone.trim() : null;
        this.receiverAddress = receiverAddress != null ? receiverAddress.trim() : null;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName != null ? receiverName.trim() : null;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone != null ? receiverPhone.trim() : null;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress != null ? receiverAddress.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryAddress that = (DeliveryAddress) o;
        return Objects.equals(receiverName, that.receiverName) &&
                Objects.equals(receiverPhone, that.receiverPhone) &&
                Objects.equals(receiverAddress, that.receiverAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiverName, receiverPhone, receiverAddress);
    }

    @Override
    public String toString() {
        return receiverName + " (" + receiverPhone + ") " + receiverAddress;
    }
}
