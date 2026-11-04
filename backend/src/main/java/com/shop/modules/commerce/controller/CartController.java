package com.shop.modules.commerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.result.Result;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CartSummaryVO;
import com.shop.modules.commerce.dto.UpdateCartItemCommand;
import com.shop.modules.commerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台商城-购物车", description = "购物车添加商品、数量修改、勾选状态同步、单项删除与清空")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "获取当前顾客购物车明细", description = "查询所有加购条目，并附带最新商品标题、单价、库存及勾选结算金额合计")
    @GetMapping
    public Result<CartSummaryVO> getCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        CartSummaryVO summary = cartService.getCart(userId);
        return Result.success(summary);
    }

    @Operation(summary = "添加商品至购物车", description = "如果该商品已在购物车中，则自动累加购买数量并校验总库存")
    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody AddCartCommand cmd) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.addItem(userId, cmd);
        return Result.success("成功加入购物车", null);
    }

    @Operation(summary = "修改购物车项数量或勾选状态", description = "支持单独修改数量或勾选状态，修改数量时严密核验当前剩余库存")
    @PutMapping("/items/{id}")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody UpdateCartItemCommand cmd) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.updateItem(userId, id, cmd.getQuantity(), cmd.getChecked());
        return Result.success("购物车项更新成功", null);
    }

    @Operation(summary = "移除指定购物车项", description = "从当前顾客购物车中彻底移除某一项商品记录")
    @DeleteMapping("/items/{id}")
    public Result<Void> removeItem(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.removeItem(userId, id);
        return Result.success("移除成功", null);
    }

    @Operation(summary = "一键清空购物车", description = "清空当前顾客名下的全部购物车行项")
    @DeleteMapping
    public Result<Void> clearCart() {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.clearCart(userId);
        return Result.success("购物车已全部清空", null);
    }
}
