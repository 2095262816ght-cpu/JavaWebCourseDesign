package com.shop.modules.catalog.service;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.HomeShowcaseVO;
import com.shop.modules.catalog.dto.ProductCardVO;
import com.shop.modules.catalog.dto.ProductDetailVO;
import com.shop.modules.catalog.dto.ProductQueryCommand;

public interface ProductCatalogService {

    /**
     * 获取商城首页聚合橱窗数据 (轮播图、精选品牌、全量分类树、热销与新品)
     * 单次请求下发，避免前端网络瀑布流加载
     */
    HomeShowcaseVO getHomeShowcase();

    /**
     * 前台多维动态商品检索 (关键字、分类、品牌、价格区间、排序)
     */
    PageResult<ProductCardVO> searchProducts(ProductQueryCommand cmd);

    /**
     * 获取指定商品的前台展示详情 (包含画廊图片解析、所属品牌及类目元信息)
     */
    ProductDetailVO getProductDetail(Long id);
}
