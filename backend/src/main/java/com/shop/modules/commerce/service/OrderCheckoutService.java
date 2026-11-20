package com.shop.modules.commerce.service;

import com.shop.common.result.PageResult;
import com.shop.modules.commerce.dto.AdminOrderQuery;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.dto.OrderDetailVO;
import com.shop.modules.commerce.dto.OrderSummaryVO;

/**
 * 订单与结算深度模块服务接口
 * 封装完整的并发控制、库存单句扣减、快照归档与事务生命周期
 */
public interface OrderCheckoutService {

    /**
     * 原子结算下单：读取已勾选购物车项、核验价格与库存、单SQL行级锁原子扣减库存、创建订单快照并清空已购购物车
     *
     * @param customerId 顾客ID
     * @param cmd        收货信息与结算命令
     * @return 订单生成结果（含唯一订单号与应付金额）
     */
    OrderCheckoutResult checkout(Long customerId, CheckoutCommand cmd);

    /**
     * 顾客确认收货：将已发货订单流转为已完成（SHIPPED -> COMPLETED）
     *
     * @param customerId 顾客ID（必填，用于校验订单归属权）
     * @param orderId    订单ID
     */
    void confirmReceipt(Long customerId, Long orderId);

    /**
     * 顾客取消订单：未发货订单（UNPAID / PAID）流转为已取消（CANCELLED），并回补商品库存
     *
     * @param customerId 顾客ID（必填，用于校验订单归属权）
     * @param orderId    订单ID
     */
    void cancelOrder(Long customerId, Long orderId);

    /**
     * 管理员取消订单：不校验归属权，未发货订单（UNPAID / PAID）流转为已取消（CANCELLED），并回补商品库存
     *
     * @param orderId 订单ID
     */
    void cancelOrderByAdmin(Long orderId);

    /**
     * 模拟订单支付：将未支付订单置为已支付并记录支付时间
     *
     * @param customerId 顾客ID（必填，用于校验订单归属权）
     * @param orderNo    业务订单编号
     */
    void pay(Long customerId, String orderNo);

    /**
     * 顾客个人订单历史列表查询
     *
     * @param customerId 顾客ID
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @return 分页订单列表
     */
    default PageResult<OrderSummaryVO> listCustomerOrders(Long customerId, int pageNum, int pageSize) {
        return listCustomerOrders(customerId, null, pageNum, pageSize);
    }

    /**
     * 顾客个人订单历史列表查询（支持按订单状态筛选）
     *
     * @param customerId 顾客ID
     * @param status     订单状态（可选）
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @return 分页订单列表
     */
    PageResult<OrderSummaryVO> listCustomerOrders(Long customerId, String status, int pageNum, int pageSize);

    /**
     * 顾客获取本人订单详情（包含全部下单快照行项与物流收件信息）
     *
     * @param customerId 顾客ID（必填，强制校验订单归属权）
     * @param orderId    订单ID
     * @return 订单详情全量VO
     */
    OrderDetailVO getOrderDetail(Long customerId, Long orderId);

    /**
     * 管理员获取全平台任意订单详情（不限制归属权）
     *
     * @param orderId 订单ID
     * @return 订单详情全量VO
     */
    OrderDetailVO getAdminOrderDetail(Long orderId);

    /**
     * 管理员标记订单发货（流转状态为 SHIPPED）
     *
     * @param orderId 订单ID
     */
    void markShipped(Long orderId);

    /**
     * 管理后台分页检索全平台订单
     *
     * @param query 组合过滤检索参数
     * @return 分页订单列表
     */
    PageResult<OrderSummaryVO> listAdminOrders(AdminOrderQuery query);
}
