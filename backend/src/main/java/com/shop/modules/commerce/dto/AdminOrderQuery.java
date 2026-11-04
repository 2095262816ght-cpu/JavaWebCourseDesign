package com.shop.modules.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理后台订单复合检索入参")
public class AdminOrderQuery {

    @Schema(description = "订单号模糊匹配", example = "ORD2026")
    private String orderNo;

    @Schema(description = "订单状态", example = "PAID")
    private String status;

    @Schema(description = "收货人手机号模糊匹配", example = "13800138000")
    private String receiverPhone;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    public AdminOrderQuery() {
    }

    public AdminOrderQuery(String orderNo, String status, String receiverPhone, Integer pageNum, Integer pageSize) {
        this.orderNo = orderNo;
        this.status = status;
        this.receiverPhone = receiverPhone;
        this.pageNum = (pageNum != null && pageNum > 0) ? pageNum : 1;
        this.pageSize = (pageSize != null && pageSize > 0) ? pageSize : 10;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
