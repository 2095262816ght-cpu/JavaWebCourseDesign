package com.shop.modules.commerce;

import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.AdminOrderQuery;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.dto.OrderDetailVO;
import com.shop.modules.commerce.dto.OrderSummaryVO;
import com.shop.modules.commerce.entity.ShopCartItem;
import com.shop.modules.commerce.entity.ShopOrder;
import com.shop.modules.commerce.mapper.ShopCartItemMapper;
import com.shop.modules.commerce.mapper.ShopOrderMapper;
import com.shop.modules.commerce.service.CartService;
import com.shop.modules.commerce.service.OrderCheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderCheckoutServiceTest {

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ShopProductMapper productMapper;

    @Autowired
    private ShopOrderMapper orderMapper;

    @Autowired
    private ShopCartItemMapper cartItemMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("测试完整结算生命周期：下单、扣减库存、快照持久化、清空已购购物车、支付与发货")
    void testFullCheckoutCommerceLifecycle() {
        Long testUserId = 3001L;

        // 预热商品详情缓存
        redisTemplate.opsForValue().set("catalog:product:detail:1", "{\"mock\":\"cached\"}");
        assertThat(redisTemplate.hasKey("catalog:product:detail:1")).isTrue();

        // 1. 准备购物车数据：加购 2 件商品 1（iPhone 16 Pro Max，价格 9999.00）
        ShopProduct beforeProduct = productMapper.selectById(1L);
        int initialStock = beforeProduct.getStock();

        cartService.addItem(testUserId, new AddCartCommand(1L, 2));

        // 2. 提交结算
        CheckoutCommand checkoutCmd = new CheckoutCommand("张同学", "13812345678", "北京市海淀区中关村南大街5号");
        OrderCheckoutResult checkoutResult = orderCheckoutService.checkout(testUserId, checkoutCmd);

        assertThat(checkoutResult).isNotNull();
        assertThat(checkoutResult.getOrderNo()).startsWith("ORD");
        assertThat(checkoutResult.getStatus()).isEqualTo("UNPAID");
        assertThat(checkoutResult.getTotalAmount()).isEqualByComparingTo(new BigDecimal("19998.00"));

        // 3. 校验库存扣减：原库存 - 2，且缓存已被清除
        ShopProduct afterProduct = productMapper.selectById(1L);
        assertThat(afterProduct.getStock()).isEqualTo(initialStock - 2);
        assertThat(redisTemplate.hasKey("catalog:product:detail:1")).isFalse();

        // 4. 校验购物车已自动清空已购商品
        assertThat(cartService.getCart(testUserId).getTotalItems()).isEqualTo(0);

        // 5. 校验订单详情快照、customerId 以及 DeliveryAddress 封装
        OrderDetailVO detail = orderCheckoutService.getOrderDetail(testUserId, checkoutResult.getOrderId());
        assertThat(detail).isNotNull();
        assertThat(detail.getOrderNo()).isEqualTo(checkoutResult.getOrderNo());
        assertThat(detail.getCustomerId()).isEqualTo(testUserId);
        assertThat(detail.getReceiverName()).isEqualTo("张同学");
        assertThat(detail.getDeliveryAddress()).isNotNull();
        assertThat(detail.getDeliveryAddress().getReceiverPhone()).isEqualTo("13812345678");
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().get(0).getProductName()).isEqualTo(beforeProduct.getName());
        assertThat(detail.getItems().get(0).getUnitPrice()).isEqualByComparingTo(beforeProduct.getPrice());
        assertThat(detail.getItems().get(0).getQuantity()).isEqualTo(2);

        // 6. 模拟顾客完成订单支付
        orderCheckoutService.pay(testUserId, checkoutResult.getOrderNo());
        ShopOrder paidOrder = orderMapper.selectById(checkoutResult.getOrderId());
        assertThat(paidOrder.getStatus()).isEqualTo("PAID");
        assertThat(paidOrder.getPaymentTime()).isNotNull();

        // 7. 管理员执行发货
        orderCheckoutService.markShipped(checkoutResult.getOrderId());
        ShopOrder shippedOrder = orderMapper.selectById(checkoutResult.getOrderId());
        assertThat(shippedOrder.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("测试购物车无勾选商品时下单抛出异常")
    void testCheckoutWithoutCheckedItemsThrowsException() {
        Long testUserId = 3002L;
        // 未添加任何商品至购物车
        CheckoutCommand cmd = new CheckoutCommand("李同学", "13900000000", "上海市浦东新区张江高科");

        assertThatThrownBy(() -> orderCheckoutService.checkout(testUserId, cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车中暂无勾选的结算商品");
    }

    @Test
    @DisplayName("测试未支付订单直接标记发货抛出异常")
    void testShipUnpaidOrderThrowsException() {
        Long testUserId = 3003L;
        cartService.addItem(testUserId, new AddCartCommand(2L, 1));

        CheckoutCommand cmd = new CheckoutCommand("王同学", "13700000000", "深圳市南山区科技园");
        OrderCheckoutResult result = orderCheckoutService.checkout(testUserId, cmd);

        // 尝试直接发货未支付订单
        assertThatThrownBy(() -> orderCheckoutService.markShipped(result.getOrderId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅已支付（PAID）订单允许标记发货");
    }

    @Test
    @DisplayName("测试管理后台多维订单检索")
    void testAdminOrderQuery() {
        Long testUserId = 3004L;
        cartService.addItem(testUserId, new AddCartCommand(3L, 1));
        CheckoutCommand cmd = new CheckoutCommand("赵同学", "13699998888", "广州市天河区珠江新城");
        OrderCheckoutResult result = orderCheckoutService.checkout(testUserId, cmd);

        AdminOrderQuery query = new AdminOrderQuery();
        query.setReceiverPhone("13699998888");
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<OrderSummaryVO> pageResult = orderCheckoutService.listAdminOrders(query);
        assertThat(pageResult.records()).isNotEmpty();
        assertThat(pageResult.records().stream().anyMatch(o -> o.getOrderNo().equals(result.getOrderNo()))).isTrue();
    }
}
