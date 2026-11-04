package com.shop.modules.catalog.service;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.AdminProductQuery;
import com.shop.modules.catalog.dto.ProductAdminVO;
import com.shop.modules.catalog.dto.ProductCommand;

public interface ProductAdminService {

    /**
     * 新增或更新商品信息 (校验品牌与分类有效性，持久化主图及画廊副图)
     */
    Long saveProduct(ProductCommand cmd);

    /**
     * 快速变更商品上下架状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 管理后台商品多条件分页列表检索
     */
    PageResult<ProductAdminVO> listAdminProducts(AdminProductQuery query);

    /**
     * 根据商品ID获取管理后台详情
     */
    ProductAdminVO getAdminProductById(Long id);

    /**
     * 逻辑删除指定商品
     */
    void deleteProduct(Long id);
}
