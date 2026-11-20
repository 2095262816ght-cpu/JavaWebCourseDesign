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
import com.shop.modules.commerce.service.ProductBatchLookupHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    private final ShopCartItemMapper cartItemMapper;
    private final ShopProductMapper productMapper;
    private final ProductBatchLookupHelper productBatchLookupHelper;

    public CartServiceImpl(ShopCartItemMapper cartItemMapper,
                           ShopProductMapper productMapper,
                           ProductBatchLookupHelper productBatchLookupHelper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productBatchLookupHelper = productBatchLookupHelper;
    }

    @Override
    public CartSummaryVO getCart(Long customerId) {
        if (customerId == null) {
            return new CartSummaryVO(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getCustomerId, customerId)
                .orderByDesc(ShopCartItem::getCreatedAt);
        List<ShopCartItem> cartItems = cartItemMapper.selectList(queryWrapper);

        if (cartItems.isEmpty()) {
            return new CartSummaryVO(Collections.emptyList(), 0, 0, BigDecimal.ZERO);
        }

        List<Long> productIds = cartItems.stream()
                .map(ShopCartItem::getProductId)
                .toList();

        Map<Long, ShopProduct> productMap = productBatchLookupHelper.getProductMapByIds(productIds);

        List<CartItemVO> voList = new ArrayList<>(cartItems.size());
        int checkedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ShopCartItem item : cartItems) {
            ShopProduct product = productMap.get(item.getProductId());
            boolean isProductValid = product != null && product.getIsDeleted() == 0;
            BigDecimal price = (isProductValid && product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = item.calculateSubtotal(price);
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

            if (isChecked && isProductValid && Integer.valueOf(1).equals(product.getStatus())
                    && product.getStock() != null && product.getStock() > 0) {
                checkedCount += item.getQuantity();
                totalAmount = totalAmount.add(subtotal);
            }
        }

        return new CartSummaryVO(voList, voList.size(), checkedCount, totalAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long customerId, AddCartCommand cmd) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopProduct product = productMapper.selectById(cmd.getProductId());
        if (product == null || !Integer.valueOf(0).equals(product.getIsDeleted()) || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("该商品不存在或已下架");
        }

        int addQty = (cmd.getQuantity() != null && cmd.getQuantity() > 0) ? cmd.getQuantity() : 1;

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getCustomerId, customerId)
                .eq(ShopCartItem::getProductId, cmd.getProductId());
        ShopCartItem existing = cartItemMapper.selectOne(queryWrapper);

        int availableStock = product.getStock() != null ? product.getStock() : 0;
        int currentQty = existing != null ? existing.getQuantity() : 0;
        int targetQty = currentQty + addQty;
        if (targetQty > availableStock) {
            String prefix = existing != null ? "加购数量" : "购买数量";
            throw new BusinessException(prefix + "超过商品可用库存（当前库存：" + availableStock + " 件）");
        }

        if (existing != null) {
            existing.setQuantity(targetQty);
            existing.setChecked(1);
            cartItemMapper.updateById(existing);
        } else {
            ShopCartItem newItem = new ShopCartItem();
            newItem.setCustomerId(customerId);
            newItem.setProductId(cmd.getProductId());
            newItem.setQuantity(targetQty);
            newItem.setChecked(1);
            cartItemMapper.insert(newItem);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long customerId, Long cartItemId, Integer quantity, Boolean checked) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopCartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !customerId.equals(item.getCustomerId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "未找到该购物车记录");
        }

        ShopProduct product = productMapper.selectById(item.getProductId());
        boolean isProductAvailable = product != null
                && Integer.valueOf(0).equals(product.getIsDeleted())
                && Integer.valueOf(1).equals(product.getStatus());

        if (quantity != null) {
            if (quantity <= 0) {
                throw new BusinessException("购买数量必须大于0");
            }
            if (!isProductAvailable) {
                throw new BusinessException("该商品已下架或不存在，无法修改购买数量");
            }
            int availableStock = product.getStock() != null ? product.getStock() : 0;
            if (availableStock <= 0) {
                throw new BusinessException("该商品当前已售罄，无法调整数量");
            }
            if (quantity > availableStock) {
                throw new BusinessException("修改数量不可超过商品库存（当前库存：" + availableStock + " 件）");
            }
            item.setQuantity(quantity);
        }

        if (checked != null) {
            if (Boolean.TRUE.equals(checked)) {
                if (!isProductAvailable) {
                    throw new BusinessException("该商品已下架或不存在，不可勾选结算");
                }
                int availableStock = product.getStock() != null ? product.getStock() : 0;
                if (availableStock <= 0) {
                    throw new BusinessException("该商品当前已售罄，不可勾选结算");
                }
                if (item.getQuantity() > availableStock) {
                    throw new BusinessException("购买数量超过当前库存（" + availableStock + " 件），请先调整数量后再勾选");
                }
            }
            item.setChecked(checked ? 1 : 0);
        }

        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long customerId, Long cartItemId) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ShopCartItem item = cartItemMapper.selectById(cartItemId);
        if (item != null && customerId.equals(item.getCustomerId())) {
            cartItemMapper.deleteById(cartItemId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long customerId) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LambdaQueryWrapper<ShopCartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShopCartItem::getCustomerId, customerId);
        cartItemMapper.delete(queryWrapper);
    }
}
