package com.shop.modules.commerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.dto.OrderDetailVO;
import com.shop.modules.commerce.dto.OrderSummaryVO;
import com.shop.modules.commerce.service.OrderCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台商城-订单结算", description = "购物车原子结算、模拟支付、顾客订单历史与订单快照详情")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderCheckoutService orderCheckoutService;

    public OrderController(OrderCheckoutService orderCheckoutService) {
        this.orderCheckoutService = orderCheckoutService;
    }

    @Operation(summary = "提交购物车结算下单", description = "读取已勾选购物车条目，执行单SQL原子库存扣减，生成订单快照并清空购物车")
    @PostMapping("/checkout")
    public Result<OrderCheckoutResult> checkout(@Valid @RequestBody CheckoutCommand cmd) {
        Long customerId = StpUtil.getLoginIdAsLong();
        OrderCheckoutResult result = orderCheckoutService.checkout(customerId, cmd);
        return Result.success("下单成功", result);
    }

    @Operation(summary = "模拟订单支付", description = "将 UNPAID 状态订单流转为 PAID，并记录支付成功时间")
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo) {
        Long customerId = StpUtil.getLoginIdAsLong();
        orderCheckoutService.pay(customerId, orderNo);
        return Result.success("支付成功", null);
    }

    @Operation(summary = "确认收货", description = "顾客确认已收到货物，订单状态由 SHIPPED 流转为 COMPLETED")
    @PatchMapping("/{id}/confirm")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        Long customerId = StpUtil.getLoginIdAsLong();
        orderCheckoutService.confirmReceipt(customerId, id);
        return Result.success("确认收货成功", null);
    }

    @Operation(summary = "取消订单", description = "顾客取消未发货（UNPAID / PAID）订单，状态流转为 CANCELLED 并回补库存")
    @PatchMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        Long customerId = StpUtil.getLoginIdAsLong();
        orderCheckoutService.cancelOrder(customerId, id);
        return Result.success("订单已取消", null);
    }

    @Operation(summary = "获取当前顾客的订单历史", description = "按创建时间倒序分页展示订单卡片及包含的商品快照")
    @GetMapping("/my")
    public Result<PageResult<OrderSummaryVO>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Long customerId = StpUtil.getLoginIdAsLong();
        PageResult<OrderSummaryVO> pageResult = orderCheckoutService.listCustomerOrders(customerId, status, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取指定订单详情", description = "查看订单详细收件信息、商品行项快照与金额详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        Long customerId = StpUtil.getLoginIdAsLong();
        OrderDetailVO detail = orderCheckoutService.getOrderDetail(customerId, id);
        return Result.success(detail);
    }
}
