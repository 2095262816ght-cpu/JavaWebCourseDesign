package com.shop.modules.commerce.service;

import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CartSummaryVO;

/**
 * 购物车核心服务契约（高内聚深模块）
 */
public interface CartService {

    /**
     * 获取顾客购物车全部明细、勾选统计及实时小计金额
     *
     * @param userId 顾客用户ID
     * @return 购物车聚合视图
     */
    CartSummaryVO getCart(Long userId);

    /**
     * 添加商品到购物车（若已存在该商品行项，则原子累加数量并校验库存）
     *
     * @param userId 顾客用户ID
     * @param cmd    加购命令
     */
    void addItem(Long userId, AddCartCommand cmd);

    /**
     * 更新购物车行项数量或勾选状态
     *
     * @param userId     顾客用户ID
     * @param cartItemId 购物车记录ID
     * @param quantity   新数量（可为 null）
     * @param checked    新勾选状态（可为 null）
     */
    void updateItem(Long userId, Long cartItemId, Integer quantity, Boolean checked);

    /**
     * 移除购物车单项
     *
     * @param userId     顾客用户ID
     * @param cartItemId 购物车记录ID
     */
    void removeItem(Long userId, Long cartItemId);

    /**
     * 清空顾客购物车
     *
     * @param userId 顾客用户ID
     */
    void clearCart(Long userId);
}
