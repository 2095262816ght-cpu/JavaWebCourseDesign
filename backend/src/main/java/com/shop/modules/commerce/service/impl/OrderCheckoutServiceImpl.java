package com.shop.modules.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.commerce.dto.AdminOrderQuery;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.dto.OrderDetailVO;
import com.shop.modules.commerce.dto.OrderItemVO;
import com.shop.modules.commerce.dto.OrderSummaryVO;
import com.shop.modules.commerce.entity.ShopCartItem;
import com.shop.modules.commerce.entity.ShopOrder;
import com.shop.modules.commerce.entity.ShopOrderItem;
import com.shop.modules.commerce.mapper.ShopCartItemMapper;
import com.shop.modules.commerce.mapper.ShopOrderItemMapper;
import com.shop.modules.commerce.mapper.ShopOrderMapper;
import com.shop.modules.commerce.service.OrderCheckoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    private final ShopCartItemMapper cartItemMapper;
    private final ShopProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;

    public OrderCheckoutServiceImpl(ShopCartItemMapper cartItemMapper,
                                    ShopProductMapper productMapper,
                                    ShopOrderMapper orderMapper,
                                    ShopOrderItemMapper orderItemMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCheckoutResult checkout(Long userId, CheckoutCommand cmd) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 1. 读取顾客已勾选的购物车项
        LambdaQueryWrapper<ShopCartItem> cartQuery = new LambdaQueryWrapper<>();
        cartQuery.eq(ShopCartItem::getUserId, userId)
                .eq(ShopCartItem::getChecked, 1);
        List<ShopCartItem> cartItems = cartItemMapper.selectList(cartQuery);

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车中暂无勾选的结算商品");
        }

        List<Long> productIds = cartItems.stream()
                .map(ShopCartItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        List<ShopProduct> products = productMapper.selectBatchIds(productIds);
        Map<Long, ShopProduct> productMap = products.stream()
                .collect(Collectors.toMap(ShopProduct::getId, p -> p, (k1, k2) -> k1));

        // 2. 校验商品有效性与单句行级锁原子扣减库存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShopCartItem item : cartItems) {
            ShopProduct product = productMap.get(item.getProductId());
            if (product == null || product.getIsDeleted() == 1 || !Integer.valueOf(1).equals(product.getStatus())) {
                throw new BusinessException("商品 [" + (product != null ? product.getName() : item.getProductId()) + "] 已下架或不存在");
            }

            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品 [" + product.getName() + "] 库存不足（当前库存：" +
                        (product.getStock() != null ? product.getStock() : 0) + " 件）");
            }

            // 单SQL原子条件扣减：UPDATE shop_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}
            int affectedRows = productMapper.decrementStock(product.getId(), item.getQuantity());
            if (affectedRows == 0) {
                throw new BusinessException("商品 [" + product.getName() + "] 库存不足，扣减失败");
            }

            BigDecimal lineSubtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineSubtotal);
        }

        // 3. 生成唯一订单编号并持久化订单主表
        String orderNo = generateOrderNo();
        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setReceiverName(cmd.getReceiverName().trim());
        order.setReceiverPhone(cmd.getReceiverPhone().trim());
        order.setReceiverAddress(cmd.getReceiverAddress().trim());
        order.setStatus("UNPAID");
        orderMapper.insert(order);

        // 4. 生成订单项不可变价格与图片快照并保存
        for (ShopCartItem item : cartItems) {
            ShopProduct product = productMap.get(item.getProductId());
            ShopOrderItem orderItem = new ShopOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(order.getOrderNo());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        // 5. 结算成功后物理清空已勾选的购物车记录
        List<Long> checkedCartIds = cartItems.stream().map(ShopCartItem::getId).collect(Collectors.toList());
        cartItemMapper.deleteBatchIds(checkedCartIds);

        return new OrderCheckoutResult(order.getId(), order.getOrderNo(), order.getTotalAmount(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException("订单编号不能为空");
        }

        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopOrder::getOrderNo, orderNo.trim());
        ShopOrder order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到该订单");
        }

        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作该订单");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        if (!"UNPAID".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("订单当前状态不可支付：" + order.getStatus());
        }

        order.setStatus("PAID");
        order.setPaymentTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public PageResult<OrderSummaryVO> listCustomerOrders(Long userId, int pageNum, int pageSize) {
        if (userId == null) {
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        }

        Page<ShopOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopOrder::getUserId, userId)
                .orderByDesc(ShopOrder::getCreatedAt);

        Page<ShopOrder> resultPage = orderMapper.selectPage(page, queryWrapper);
        List<ShopOrder> orders = resultPage.getRecords();

        if (orders.isEmpty()) {
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        }

        List<OrderSummaryVO> summaryVOList = enrichOrdersWithItems(orders);
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), summaryVOList);
    }

    @Override
    public OrderDetailVO getOrderDetail(Long userId, Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单ID不能为空");
        }

        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单记录不存在");
        }

        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看他人订单");
        }

        LambdaQueryWrapper<ShopOrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(ShopOrderItem::getOrderId, orderId);
        List<ShopOrderItem> items = orderItemMapper.selectList(itemQuery);

        List<OrderItemVO> itemVOList = items.stream()
                .map(this::toOrderItemVO)
                .collect(Collectors.toList());

        return new OrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getPaymentTime(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemVOList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markShipped(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单ID不能为空");
        }

        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }

        if (!"PAID".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("仅已支付（PAID）订单允许标记发货，当前状态为：" + order.getStatus());
        }

        order.setStatus("SHIPPED");
        orderMapper.updateById(order);
    }

    @Override
    public PageResult<OrderSummaryVO> listAdminOrders(AdminOrderQuery query) {
        int pageNum = (query != null && query.getPageNum() != null) ? query.getPageNum() : 1;
        int pageSize = (query != null && query.getPageSize() != null) ? query.getPageSize() : 10;

        Page<ShopOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null) {
            if (StringUtils.hasText(query.getOrderNo())) {
                queryWrapper.like(ShopOrder::getOrderNo, query.getOrderNo().trim());
            }
            if (StringUtils.hasText(query.getStatus())) {
                queryWrapper.eq(ShopOrder::getStatus, query.getStatus().trim());
            }
            if (StringUtils.hasText(query.getReceiverPhone())) {
                queryWrapper.like(ShopOrder::getReceiverPhone, query.getReceiverPhone().trim());
            }
        }
        queryWrapper.orderByDesc(ShopOrder::getCreatedAt);

        Page<ShopOrder> resultPage = orderMapper.selectPage(page, queryWrapper);
        List<ShopOrder> orders = resultPage.getRecords();

        if (orders.isEmpty()) {
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        }

        List<OrderSummaryVO> summaryVOList = enrichOrdersWithItems(orders);
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), summaryVOList);
    }

    private List<OrderSummaryVO> enrichOrdersWithItems(List<ShopOrder> orders) {
        List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> items = orderItemMapper.selectList(itemQuery);

        Map<Long, List<OrderItemVO>> itemVOMap = items.stream()
                .map(this::toOrderItemVO)
                .collect(Collectors.groupingBy(OrderItemVO::getOrderId));

        List<OrderSummaryVO> summaryList = new ArrayList<>(orders.size());
        for (ShopOrder order : orders) {
            List<OrderItemVO> itemVOs = itemVOMap.getOrDefault(order.getId(), Collections.emptyList());
            int itemCount = itemVOs.stream().mapToInt(OrderItemVO::getQuantity).sum();

            OrderSummaryVO summaryVO = new OrderSummaryVO(
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    order.getReceiverName(),
                    order.getReceiverPhone(),
                    order.getReceiverAddress(),
                    order.getPaymentTime(),
                    order.getCreatedAt(),
                    itemCount,
                    itemVOs
            );
            summaryList.add(summaryVO);
        }
        return summaryList;
    }

    private OrderItemVO toOrderItemVO(ShopOrderItem item) {
        return new OrderItemVO(
                item.getId(),
                item.getOrderId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductImage(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }

    private String generateOrderNo() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timestamp + randomSuffix;
    }
}
