package com.shop.modules.commerce.service;

import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductBatchLookupHelper {

    private final ShopProductMapper productMapper;

    public ProductBatchLookupHelper(ShopProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Map<Long, ShopProduct> getProductMapByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctIds = productIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ShopProduct> products = productMapper.selectBatchIds(distinctIds);
        return products.stream()
                .collect(Collectors.toMap(ShopProduct::getId, p -> p, (k1, k2) -> k1));
    }
}
