package com.shop.modules.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.modules.commerce.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {

    /**
     * 以条件更新原子地将未发货订单置为已取消，用于并发取消时抢占状态迁移权
     * UPDATE shop_order SET status = 'CANCELLED' WHERE id = #{orderId} AND status IN ('UNPAID', 'PAID')
     * @return 实际影响行数（1 表示本次取消成功，0 表示订单状态已变更）
     */
    @Update("UPDATE shop_order SET status = 'CANCELLED' WHERE id = #{orderId} AND status IN ('UNPAID', 'PAID')")
    int cancelUnshippedOrder(@Param("orderId") Long orderId);

    /**
     * 以条件更新原子地将未支付订单置为已支付，避免并发支付或与取消操作竞争时重复写入
     * UPDATE shop_order SET status = 'PAID', payment_time = #{paymentTime} WHERE id = #{orderId} AND status = 'UNPAID'
     * @return 实际影响行数（1 表示本次支付成功，0 表示订单状态已变更）
     */
    @Update("UPDATE shop_order SET status = 'PAID', payment_time = #{paymentTime} WHERE id = #{orderId} AND status = 'UNPAID'")
    int payUnpaidOrder(@Param("orderId") Long orderId, @Param("paymentTime") LocalDateTime paymentTime);

    /**
     * 以条件更新原子地将已支付订单置为已发货，避免并发发货或与取消操作竞争
     * UPDATE shop_order SET status = 'SHIPPED' WHERE id = #{orderId} AND status = 'PAID'
     * @return 实际影响行数（1 表示本次发货成功，0 表示订单状态已变更）
     */
    @Update("UPDATE shop_order SET status = 'SHIPPED' WHERE id = #{orderId} AND status = 'PAID'")
    int shipPaidOrder(@Param("orderId") Long orderId);

    /**
     * 当前读订单状态（锁定读），用于条件更新失败后判定订单的最新真实状态
     */
    @Select("SELECT status FROM shop_order WHERE id = #{orderId} FOR UPDATE")
    String selectStatusForUpdate(@Param("orderId") Long orderId);
}
