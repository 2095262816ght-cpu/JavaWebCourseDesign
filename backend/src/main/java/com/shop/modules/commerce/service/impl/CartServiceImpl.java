package com.shop.modules.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CartItemVO;
import com.shop.modules.commerce.dto.CartSummaryVO;
import com.shop.modules.commerce.entity.ShopCartItem;
import com.shop.modules.commerce.mapper.ShopCartItemMapper;
import com.shop.modules.commerce.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final ShopCartItemMapper cartItemMapper;
    private final ShopProductMapper productMapper;

    public CartServiceImpl(ShopCartItemMapper cartItemMapper, ShopProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    @Override
    public CartSummaryVO getCart(Long userId) {
        if (userId == null) {
            return new CartSummaryVO(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getUserId, userId)
                .orderByDesc(ShopCartItem::getCreatedAt);
        List<ShopCartItem> cartItems = cartItemMapper.selectList(queryWrapper);

        if (cartItems.isEmpty()) {
            return new CartSummaryVO(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        List<Long> productIds = cartItems.stream()
                .map(ShopCartItem::getProductId)
                .distinct()
                .toList();

        List<ShopProduct> products = productMapper.selectBatchIds(productIds);
        Map<Long, ShopProduct> productMap = products.stream()
                .collect(Collectors.toMap(ShopProduct::getId, p -> p, (k1, k2) -> k1));

        List<CartItemVO> voList = new ArrayList<>(cartItems.size());
        int checkedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ShopCartItem item : cartItems) {
            ShopProduct product = productMap.get(item.getProductId());
            boolean isProductValid = product != null && product.getIsDeleted() == 0;
            BigDecimal price = (isProductValid && product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            boolean isChecked = Integer.valueOf(1).equals(item.getChecked());

            CartItemVO vo = new CartItemVO(
                    item.getId(),
                    item.getProductId(),
                    isProductValid ? product.getName() : "已失效商品",
                    isProductValid ? product.getMainImage() : "",
                    price,
                    item.getQuantity(),
                    subtotal,
                    isProductValid ? product.getStock() : 0,
                    isChecked,
                    isProductValid ? product.getStatus() : 0
            );
            voList.add(vo);

            if (isChecked && isProductValid && Integer.valueOf(1).equals(product.getStatus())) {
                checkedCount += item.getQuantity();
                totalAmount = totalAmount.add(subtotal);
            }
        }

        return new CartSummaryVO(voList, voList.size(), checkedCount, totalAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long userId, AddCartCommand cmd) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopProduct product = productMapper.selectById(cmd.getProductId());
        if (product == null || product.getIsDeleted() == 1 || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("该商品不存在或已下架");
        }

        int addQty = (cmd.getQuantity() != null && cmd.getQuantity() > 0) ? cmd.getQuantity() : 1;

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getUserId, userId)
                .eq(ShopCartItem::getProductId, cmd.getProductId());
        ShopCartItem existing = cartItemMapper.selectOne(queryWrapper);

        int currentQty = existing != null ? existing.getQuantity() : 0;
        int targetQty = currentQty + addQty;
        if (targetQty > product.getStock()) {
            String prefix = existing != null ? "加购数量" : "购买数量";
            throw new BusinessException(prefix + "超过商品可用库存（当前库存：" + product.getStock() + " 件）");
        }

        if (existing != null) {
            existing.setQuantity(targetQty);
            existing.setChecked(1);
            cartItemMapper.updateById(existing);
        } else {
            ShopCartItem newItem = new ShopCartItem();
            newItem.setUserId(userId);
            newItem.setProductId(cmd.getProductId());
            newItem.setQuantity(targetQty);
            newItem.setChecked(1);
            cartItemMapper.insert(newItem);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long userId, Long cartItemId, Integer quantity, Boolean checked) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopCartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到该购物车记录");
        }

        if (quantity != null) {
            if (quantity <= 0) {
                throw new BusinessException("购买数量必须大于0");
            }
            ShopProduct product = productMapper.selectById(item.getProductId());
            if (product != null && quantity > product.getStock()) {
                throw new BusinessException("修改数量不可超过商品库存（当前库存：" + product.getStock() + " 件）");
            }
            item.setQuantity(quantity);
        }

        if (checked != null) {
            item.setChecked(checked ? 1 : 0);
        }

        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long cartItemId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopCartItem item = cartItemMapper.selectById(cartItemId);
        if (item != null && userId.equals(item.getUserId())) {
            cartItemMapper.deleteById(cartItemId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getUserId, userId);
        cartItemMapper.delete(queryWrapper);
    }
}
