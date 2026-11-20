package com.shop.modules.catalog.service;

import com.shop.common.result.PageResult;
import com.shop.modules.catalog.dto.BrandCommand;
import com.shop.modules.catalog.dto.BrandQuery;
import com.shop.modules.catalog.dto.BrandVO;

import java.util.List;

public interface BrandService {

    /**
     * 获取推荐品牌列表（内置 Cache-Aside 缓存加速）
     *
     * @param limit 获取数量限制
     * @return 推荐品牌列表
     */
    List<BrandVO> getFeaturedBrands(int limit);

    /**
     * 获取前台全部启用品牌列表（无数量上限，供筛选与目录展示，内置 Cache-Aside 缓存加速）
     *
     * @return 全量启用品牌列表
     */
    List<BrandVO> getPortalBrands();

    /**
     * 分页多条件查询品牌列表
     *
     * @param query 查询参数
     * @return 品牌分页结果
     */
    PageResult<BrandVO> listBrands(BrandQuery query);

    /**
     * 根据ID获取品牌详情
     *
     * @param id 品牌ID
     * @return 品牌详情
     */
    BrandVO getBrandById(Long id);

    /**
     * 新增或更新品牌（执行后自动失效推荐品牌缓存）
     *
     * @param cmd 品牌参数
     */
    void saveBrand(BrandCommand cmd);

    /**
     * 删除指定品牌（执行后自动失效推荐品牌缓存）
     *
     * @param id 品牌ID
     */
    void deleteBrand(Long id);
}
