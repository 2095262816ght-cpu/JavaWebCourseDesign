package com.shop.modules.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.cache.TransactionalCacheEvictor;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
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
import com.shop.modules.commerce.service.ProductBatchLookupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private static final Logger log = LoggerFactory.getLogger(OrderCheckoutServiceImpl.class);
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ShopCartItemMapper cartItemMapper;
    private final ShopProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;
    private final ProductBatchLookupHelper productBatchLookupHelper;
    private final StringRedisTemplate redisTemplate;

    public OrderCheckoutServiceImpl(ShopCartItemMapper cartItemMapper,
                                    ShopProductMapper productMapper,
                                    ShopOrderMapper orderMapper,
                                    ShopOrderItemMapper orderItemMapper,
                                    ProductBatchLookupHelper productBatchLookupHelper,
                                    StringRedisTemplate redisTemplate) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productBatchLookupHelper = productBatchLookupHelper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCheckoutResult checkout(Long customerId, CheckoutCommand cmd) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 1. 读取顾客已勾选的购物车项
        LambdaQueryWrapper<ShopCartItem> cartQuery = new LambdaQueryWrapper<>();
        cartQuery.eq(ShopCartItem::getCustomerId, customerId)
                .eq(ShopCartItem::getChecked, 1);
        List<ShopCartItem> cartItems = cartItemMapper.selectList(cartQuery);

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车中暂无勾选的结算商品");
        }

        List<Long> productIds = cartItems.stream()
                .map(ShopCartItem::getProductId)
                .toList();

        Map<Long, ShopProduct> productMap = productBatchLookupHelper.getProductMapByIds(productIds);

        // 2. 校验商品有效性与单句行级锁原子扣减库存
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShopCartItem item : cartItems) {
            ShopProduct product = productMap.get(item.getProductId());
            if (product == null || Integer.valueOf(1).equals(product.getIsDeleted()) || !Integer.valueOf(1).equals(product.getStatus())) {
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

            // 扣减成功后立即主动失效 Redis 商品详情与聚合橱窗缓存，确保前台查看实时库存 (Cache Invalidation)
            evictProductDetailCache(product.getId());

            // 消除 Feature Envy：委托 item 计算自身在指定单价下的小计金额
            BigDecimal lineSubtotal = item.calculateSubtotal(product.getPrice());
            totalAmount = totalAmount.add(lineSubtotal);
        }

        // 3. 生成唯一订单编号并持久化订单主表（封装 DeliveryAddress 数据泥团）
        String orderNo = generateOrderNo();
        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setCustomerId(customerId);
        order.setTotalAmount(totalAmount);
        order.setDeliveryAddress(cmd.getDeliveryAddress());
        order.setStatus("UNPAID");
        orderMapper.insert(order);

        // 4. 生成订单项不可变价格与图片快照并保存（消除 Feature Envy：委托 orderItem 计算小计）
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
            orderItem.setTotalPrice(orderItem.calculateSubtotal());
            orderItemMapper.insert(orderItem);
        }

        // 5. 结算成功后物理清空已勾选的购物车记录
        List<Long> checkedCartIds = cartItems.stream().map(ShopCartItem::getId).toList();
        cartItemMapper.deleteBatchIds(checkedCartIds);

        return new OrderCheckoutResult(order.getId(), order.getOrderNo(), order.getTotalAmount(), order.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long customerId, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException("订单编号不能为空");
        }

        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopOrder::getOrderNo, orderNo.trim());
        ShopOrder order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到该订单");
        }

        if (customerId == null || !customerId.equals(order.getCustomerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作该订单");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        if (!"UNPAID".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("订单当前状态不可支付：" + order.getStatus());
        }

        // 条件更新抢占状态迁移权：并发支付时只有一个请求能成功写入，避免支付时间被重复覆盖
        int affectedRows = orderMapper.payUnpaidOrder(order.getId(), LocalDateTime.now());
        if (affectedRows == 0) {
            // 条件更新失败说明状态已被并发请求改变，当前读最新状态以保持“已支付即幂等成功”的语义
            String latestStatus = orderMapper.selectStatusForUpdate(order.getId());
            if ("PAID".equalsIgnoreCase(latestStatus)) {
                return;
            }
            throw new BusinessException("订单状态已变更，无法支付，请刷新后重试");
        }
    }

    @Override
    public PageResult<OrderSummaryVO> listCustomerOrders(Long customerId, String status, int pageNum, int pageSize) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        if (customerId == null) {
            return PageResult.of(safePageNum, safePageSize, 0L, Collections.emptyList());
        }

        Page<ShopOrder> page = new Page<>(safePageNum, safePageSize);
        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopOrder::getCustomerId, customerId);
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(ShopOrder::getStatus, status.trim().toUpperCase());
        }
        queryWrapper.orderByDesc(ShopOrder::getCreatedAt)
                .orderByDesc(ShopOrder::getId);

        Page<ShopOrder> resultPage = orderMapper.selectPage(page, queryWrapper);
        List<ShopOrder> orders = resultPage.getRecords();

        if (orders.isEmpty()) {
            return PageResult.of(safePageNum, safePageSize, 0L, Collections.emptyList());
        }

        List<OrderSummaryVO> summaryVOList = enrichOrdersWithItems(orders);
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), summaryVOList);
    }

    @Override
    public OrderDetailVO getOrderDetail(Long customerId, Long orderId) {
        // 顾客路径强制要求 customerId，并校验订单归属，避免漏传参数退化为无鉴权查询
        return buildOrderDetail(requireOwnedOrder(customerId, orderId));
    }

    @Override
    public OrderDetailVO getAdminOrderDetail(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单ID不能为空");
        }

        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单记录不存在");
        }

        return buildOrderDetail(order);
    }

    private OrderDetailVO buildOrderDetail(ShopOrder order) {
        Long orderId = order.getId();
        LambdaQueryWrapper<ShopOrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(ShopOrderItem::getOrderId, orderId);
        List<ShopOrderItem> items = orderItemMapper.selectList(itemQuery);

        List<OrderItemVO> itemVOList = items.stream()
                .map(this::toOrderItemVO)
                .toList();

        return new OrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getDeliveryAddress(),
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

        // 条件更新抢占状态迁移权：避免并发发货或与取消操作竞争导致订单状态与库存不一致
        int affectedRows = orderMapper.shipPaidOrder(orderId);
        if (affectedRows == 0) {
            String latestStatus = orderMapper.selectStatusForUpdate(orderId);
            throw new BusinessException("仅已支付（PAID）订单允许标记发货，当前状态为：" + latestStatus);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long customerId, Long orderId) {
        ShopOrder order = requireOwnedOrder(customerId, orderId);
        if (!"SHIPPED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("仅已发货（SHIPPED）订单允许确认收货，当前状态为：" + order.getStatus());
        }
        order.setStatus("COMPLETED");
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long customerId, Long orderId) {
        cancelAndRestoreStock(requireOwnedOrder(customerId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderByAdmin(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        cancelAndRestoreStock(order);
    }

    /**
     * 取消未发货订单并回补库存：仅 UNPAID / PAID 允许取消，避免已发货订单被回滚造成库存与物流不一致
     */
    private void cancelAndRestoreStock(ShopOrder order) {
        String status = order.getStatus() == null ? "" : order.getStatus().toUpperCase();
        if (!"UNPAID".equals(status) && !"PAID".equals(status)) {
            throw new BusinessException("仅未发货订单允许取消，当前状态为：" + order.getStatus());
        }

        // 先以条件更新抢占状态迁移权：并发取消时只有一个事务能把订单置为 CANCELLED，
        // 只有抢到的一方回补库存，避免库存被重复归还。
        int affectedRows = orderMapper.cancelUnshippedOrder(order.getId());
        if (affectedRows == 0) {
            throw new BusinessException("订单状态已变更，无法取消，请刷新后重试");
        }

        List<ShopOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<ShopOrderItem>().eq(ShopOrderItem::getOrderId, order.getId())
        );
        for (ShopOrderItem item : items) {
            if (item.getProductId() == null || item.getQuantity() == null) {
                continue;
            }
            productMapper.incrementStock(item.getProductId(), item.getQuantity());
            evictProductDetailCache(item.getProductId());
        }
    }

    private ShopOrder requireOwnedOrder(Long customerId, Long orderId) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!customerId.equals(order.getCustomerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作该订单");
        }
        return order;
    }

    @Override
    public PageResult<OrderSummaryVO> listAdminOrders(AdminOrderQuery query) {
        int rawPageNum = (query != null && query.getPageNum() != null) ? query.getPageNum() : 1;
        int rawPageSize = (query != null && query.getPageSize() != null) ? query.getPageSize() : 10;
        int pageNum = Math.max(rawPageNum, 1);
        int pageSize = (rawPageSize < 1) ? 10 : Math.min(rawPageSize, 100);

        Page<ShopOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShopOrder> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null) {
            if (StringUtils.hasText(query.getOrderNo())) {
                queryWrapper.like(ShopOrder::getOrderNo, query.getOrderNo().trim());
            }
            if (StringUtils.hasText(query.getStatus())) {
                queryWrapper.eq(ShopOrder::getStatus, query.getStatus().trim().toUpperCase());
            }
            if (StringUtils.hasText(query.getReceiverPhone())) {
                queryWrapper.like(ShopOrder::getReceiverPhone, query.getReceiverPhone().trim());
            }
        }
        queryWrapper.orderByDesc(ShopOrder::getCreatedAt)
                .orderByDesc(ShopOrder::getId);

        Page<ShopOrder> resultPage = orderMapper.selectPage(page, queryWrapper);
        List<ShopOrder> orders = resultPage.getRecords();

        if (orders.isEmpty()) {
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        }

        List<OrderSummaryVO> summaryVOList = enrichOrdersWithItems(orders);
        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), summaryVOList);
    }

    private void evictProductDetailCache(Long productId) {
        if (productId == null || redisTemplate == null) {
            return;
        }
        TransactionalCacheEvictor.evictNowAndAfterCommit(() -> doEvictProductDetailCache(productId));
    }

    private void doEvictProductDetailCache(Long productId) {
        try {
            redisTemplate.delete(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + productId);
            redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
        } catch (Exception e) {
            log.warn("扣减库存后清除商品详情缓存失败: productId={}, err={}", productId, e.getMessage());
        }
    }

    private List<OrderSummaryVO> enrichOrdersWithItems(List<ShopOrder> orders) {
        List<Long> orderIds = orders.stream().map(ShopOrder::getId).toList();
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
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    order.getDeliveryAddress(),
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
        String timestamp = ORDER_NO_FORMATTER.format(LocalDateTime.now());
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timestamp + randomSuffix;
    }
}
