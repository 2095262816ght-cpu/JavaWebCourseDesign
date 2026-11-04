package com.shop.modules.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.modules.catalog.entity.ShopProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopProductMapper extends BaseMapper<ShopProduct> {

    /**
     * 原子单条 SQL 扣减库存，保证高并发下零超卖
     * UPDATE shop_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity} AND is_deleted = 0
     * @return 实际影响行数（1 表示成功扣减，0 表示库存不足）
     */
    @Update("UPDATE shop_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity} AND is_deleted = 0")
    int decrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 恢复/归还库存
     */
    @Update("UPDATE shop_product SET stock = stock + #{quantity} WHERE id = #{productId} AND is_deleted = 0")
    int incrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
