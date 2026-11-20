package com.shop.modules.commerce.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.modules.commerce.dto.AdminOrderQuery;
import com.shop.modules.commerce.dto.OrderDetailVO;
import com.shop.modules.commerce.dto.OrderSummaryVO;
import com.shop.modules.commerce.service.OrderCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-订单管理", description = "系统管理员全平台订单检索、状态流转与标记发货")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderCheckoutService orderCheckoutService;

    public AdminOrderController(OrderCheckoutService orderCheckoutService) {
        this.orderCheckoutService = orderCheckoutService;
    }

    @Operation(summary = "多条件分页检索订单列表", description = "支持按订单号、订单状态以及收货人手机号进行模糊检索过滤")
    @GetMapping
    public Result<PageResult<OrderSummaryVO>> listAdminOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        AdminOrderQuery query = new AdminOrderQuery(orderNo, status, receiverPhone, pageNum, pageSize);
        PageResult<OrderSummaryVO> pageResult = orderCheckoutService.listAdminOrders(query);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取任意订单详情", description = "管理员查看订单详细收件信息、商品行项快照与金额详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getAdminOrderDetail(@PathVariable Long id) {
        OrderDetailVO detail = orderCheckoutService.getAdminOrderDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "标记订单为已发货", description = "管理员核验已支付订单后，执行发货流转将状态更改为 SHIPPED")
    @PatchMapping("/{id}/ship")
    public Result<Void> markShipped(@PathVariable Long id) {
        orderCheckoutService.markShipped(id);
        return Result.success("订单发货流转成功", null);
    }

    @Operation(summary = "取消订单", description = "管理员取消未发货（UNPAID / PAID）订单，状态流转为 CANCELLED 并回补商品库存")
    @PatchMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        orderCheckoutService.cancelOrderByAdmin(id);
        return Result.success("订单已取消", null);
    }
}
