package com.shop.modules.commerce;

import com.shop.common.exception.BusinessException;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CartSummaryVO;
import com.shop.modules.commerce.entity.ShopCartItem;
import com.shop.modules.commerce.mapper.ShopCartItemMapper;
import com.shop.modules.commerce.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ShopCartItemMapper cartItemMapper;

    @Autowired
    private ShopProductMapper productMapper;

    @Test
    @DisplayName("测试加购商品、累加数量与获取购物车汇总")
    void testAddCartAndSummary() {
        Long testUserId = 2001L;

        // 加购商品 1（iPhone 16 Pro Max，单价 9999.00）
        AddCartCommand cmd1 = new AddCartCommand(1L, 2);
        cartService.addItem(testUserId, cmd1);

        // 重复加购，数量应自动累加为 2 + 1 = 3
        AddCartCommand cmd2 = new AddCartCommand(1L, 1);
        cartService.addItem(testUserId, cmd2);

        CartSummaryVO summary = cartService.getCart(testUserId);
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalItems()).isEqualTo(1);
        assertThat(summary.getCheckedCount()).isEqualTo(3);
        assertThat(summary.getItems()).hasSize(1);
        assertThat(summary.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("29997.00"));
    }

    @Test
    @DisplayName("测试修改购物车数量与勾选状态切换")
    void testUpdateCartItem() {
        Long testUserId = 2002L;

        AddCartCommand cmd = new AddCartCommand(2L, 1); // 华为 Mate 60 Pro
        cartService.addItem(testUserId, cmd);

        CartSummaryVO summary = cartService.getCart(testUserId);
        Long itemId = summary.getItems().get(0).getId();

        // 取消勾选
        cartService.updateItem(testUserId, itemId, null, false);
        CartSummaryVO updatedSummary = cartService.getCart(testUserId);
        assertThat(updatedSummary.getCheckedCount()).isEqualTo(0);
        assertThat(updatedSummary.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedSummary.getItems().get(0).getChecked()).isFalse();

        // 重新勾选并修改数量为 3
        cartService.updateItem(testUserId, itemId, 3, true);
        CartSummaryVO recheckedSummary = cartService.getCart(testUserId);
        assertThat(recheckedSummary.getCheckedCount()).isEqualTo(3);
        assertThat(recheckedSummary.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(recheckedSummary.getItems().get(0).getChecked()).isTrue();
    }

    @Test
    @DisplayName("测试删除单项与清空购物车")
    void testRemoveAndClearCart() {
        Long testUserId = 2003L;

        cartService.addItem(testUserId, new AddCartCommand(1L, 1));
        cartService.addItem(testUserId, new AddCartCommand(2L, 1));

        CartSummaryVO summary = cartService.getCart(testUserId);
        assertThat(summary.getTotalItems()).isEqualTo(2);

        Long firstItemId = summary.getItems().get(0).getId();
        cartService.removeItem(testUserId, firstItemId);

        CartSummaryVO afterRemove = cartService.getCart(testUserId);
        assertThat(afterRemove.getTotalItems()).isEqualTo(1);

        cartService.clearCart(testUserId);
        CartSummaryVO afterClear = cartService.getCart(testUserId);
        assertThat(afterClear.getTotalItems()).isEqualTo(0);
        assertThat(afterClear.getItems()).isEmpty();
    }

    @Test
    @DisplayName("测试加购超出商品库存抛出业务异常")
    void testAddCartExceedingStock() {
        Long testUserId = 2004L;

        // 商品 1 初始库存为 50，尝试加购 999 件应抛出异常
        AddCartCommand exceedCmd = new AddCartCommand(1L, 999);
        assertThatThrownBy(() -> cartService.addItem(testUserId, exceedCmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过商品可用库存");
    }

    @Test
    @DisplayName("测试已下架或零库存商品不可增加数量或重新勾选，但允许取消勾选")
    void testUpdateInvalidOrOutOfStockCartItem() {
        Long testUserId = 2005L;

        // 先正常加购商品 2
        cartService.addItem(testUserId, new AddCartCommand(2L, 1));
        Long cartItemId = cartService.getCart(testUserId).getItems().get(0).getId();

        // 模拟管理员将商品 2 库存清零并下架
        ShopProduct product = productMapper.selectById(2L);
        product.setStock(0);
        product.setStatus(0);
        productMapper.updateById(product);

        // 此时尝试修改数量应抛出异常
        assertThatThrownBy(() -> cartService.updateItem(testUserId, cartItemId, 2, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已下架");

        // 仍应允许取消勾选，以免顾客被锁死在失效勾选状态
        cartService.updateItem(testUserId, cartItemId, null, false);
        assertThat(cartService.getCart(testUserId).getItems().get(0).getChecked()).isFalse();

        // 但不可再重新勾选已失效商品
        assertThatThrownBy(() -> cartService.updateItem(testUserId, cartItemId, null, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可勾选");

        // 恢复商品上架但保持库存为 0，仍不可勾选或修改数量
        product.setStatus(1);
        productMapper.updateById(product);
        assertThatThrownBy(() -> cartService.updateItem(testUserId, cartItemId, null, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("售罄");
        assertThatThrownBy(() -> cartService.updateItem(testUserId, cartItemId, 2, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("售罄");
    }
}
