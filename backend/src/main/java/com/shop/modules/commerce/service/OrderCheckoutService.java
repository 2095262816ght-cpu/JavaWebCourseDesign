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
     * 模拟订单支付：将未支付订单置为已支付并记录支付时间
     *
     * @param customerId 顾客ID（为空时不校验所有权，供系统或管理回调；非空时校验归属）
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
    PageResult<OrderSummaryVO> listCustomerOrders(Long customerId, int pageNum, int pageSize);

    /**
     * 获取订单详情（包含全部下单快照行项与物流收件信息）
     *
     * @param customerId 顾客ID（若提供则进行归属权安全校验）
     * @param orderId    订单ID
     * @return 订单详情全量VO
     */
    OrderDetailVO getOrderDetail(Long customerId, Long orderId);

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
