package com.shop.modules.commerce;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.entity.ShopCartItem;
import com.shop.modules.commerce.entity.ShopOrder;
import com.shop.modules.commerce.entity.ShopOrderItem;
import com.shop.modules.commerce.mapper.ShopCartItemMapper;
import com.shop.modules.commerce.mapper.ShopOrderItemMapper;
import com.shop.modules.commerce.mapper.ShopOrderMapper;
import com.shop.modules.commerce.service.OrderCheckoutService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Autowired
    private ShopProductMapper productMapper;

    @Autowired
    private ShopCartItemMapper cartItemMapper;

    @Autowired
    private ShopOrderMapper orderMapper;

    @Autowired
    private ShopOrderItemMapper orderItemMapper;

    private Long testProductId;
    private final List<Long> testUserIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // 清理并发测试产生的临时数据
        if (testProductId != null) {
            productMapper.deleteById(testProductId);
        }
        for (Long uid : testUserIds) {
            LambdaQueryWrapper<ShopCartItem> cartWrapper = new LambdaQueryWrapper<>();
            cartWrapper.eq(ShopCartItem::getUserId, uid);
            cartItemMapper.delete(cartWrapper);

            LambdaQueryWrapper<ShopOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(ShopOrder::getUserId, uid);
            List<ShopOrder> orders = orderMapper.selectList(orderWrapper);
            for (ShopOrder order : orders) {
                LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(ShopOrderItem::getOrderId, order.getId());
                orderItemMapper.delete(itemWrapper);
            }
            orderMapper.delete(orderWrapper);
        }
    }

    @Test
    @DisplayName("测试高并发原子扣减库存防超卖：10个线程同时争抢仅1件库存，严格保证1单成功9单失败且库存归零无负数")
    void testConcurrentCheckoutZeroOverselling() throws InterruptedException {
        // 1. 初始化限量秒杀商品（库存仅 1 件）
        ShopProduct product = new ShopProduct();
        product.setName("并发压测-限量智能手表（仅1件）");
        product.setSubtitle("课程大作业并发扣减深度验证");
        product.setBrandId(1L);
        product.setCategoryId(5L);
        product.setPrice(new BigDecimal("1999.00"));
        product.setOriginalPrice(new BigDecimal("2499.00"));
        product.setStock(1);
        product.setStatus(1);
        product.setMainImage("https://images.unsplash.com/test-smartwatch.png");
        productMapper.insert(product);
        this.testProductId = product.getId();

        // 2. 为 10 个独立模拟用户添加此商品的购物车记录并勾选
        int threadCount = 10;
        testUserIds.clear();
        for (int i = 0; i < threadCount; i++) {
            Long userId = 9100L + i;
            testUserIds.add(userId);

            ShopCartItem cartItem = new ShopCartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(testProductId);
            cartItem.setQuantity(1);
            cartItem.setChecked(1);
            cartItemMapper.insert(cartItem);
        }

        // 3. 构建线程池与门闩，实现 10 个线程同一瞬间并发触发 checkout()
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> failureExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final Long uid = testUserIds.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startSignal.await(); // 等待统一鸣枪起跑
                    CheckoutCommand cmd = new CheckoutCommand("抢购者-" + uid, "1380000" + uid, "并发压测收货地址");
                    OrderCheckoutResult result = orderCheckoutService.checkout(uid, cmd);
                    if (result != null && result.getOrderId() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable ex) {
                    failCount.incrementAndGet();
                    failureExceptions.add(ex);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 等待所有线程就绪
        readyLatch.await(5, TimeUnit.SECONDS);
        // 瞬间鸣枪并发执行
        startSignal.countDown();
        // 等待所有线程执行完毕
        boolean finishedInTime = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 4. 断言并发控制结果
        assertThat(finishedInTime).isTrue();
        assertThat(successCount.get()).as("高并发下抢占唯一库存，必须且只能有 1 单成功").isEqualTo(1);
        assertThat(failCount.get()).as("高并发下争抢失败的线程数量必须严格等于 9").isEqualTo(9);

        // 5. 校验数据库最终库存：必须为 0，绝对杜绝负库存或超卖
        ShopProduct finalProduct = productMapper.selectById(testProductId);
        assertThat(finalProduct.getStock()).as("唯一库存被扣减后剩余库存必须等于 0，严禁超卖产生负库存").isEqualTo(0);

        // 6. 校验失败原因均为规范的 BusinessException（库存不足）
        for (Throwable t : failureExceptions) {
            assertThat(t).isInstanceOf(BusinessException.class);
            assertThat(t.getMessage()).contains("库存不足");
        }
    }
}
