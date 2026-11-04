package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.AdminProductQuery;
import com.shop.modules.catalog.dto.ProductAdminVO;
import com.shop.modules.catalog.dto.ProductCommand;
import com.shop.modules.catalog.entity.ShopBrand;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopBrandMapper;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.ProductAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductAdminServiceImpl implements ProductAdminService {

    private static final Logger log = LoggerFactory.getLogger(ProductAdminServiceImpl.class);

    private final ShopProductMapper productMapper;
    private final ShopBrandMapper brandMapper;
    private final ShopCategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductAdminServiceImpl(ShopProductMapper productMapper,
                                   ShopBrandMapper brandMapper,
                                   ShopCategoryMapper categoryMapper,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.brandMapper = brandMapper;
        this.categoryMapper = categoryMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProduct(ProductCommand cmd) {
        if (cmd == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品参数不能为空");
        }

        // 1. 严格校验关联的品牌是否存在
        ShopBrand brand = brandMapper.selectById(cmd.brandId());
        if (brand == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "所选商品品牌不存在 (ID: " + cmd.brandId() + ")");
        }

        // 2. 严格校验关联的分类是否存在
        ShopCategory category = categoryMapper.selectById(cmd.categoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "所选商品分类不存在 (ID: " + cmd.categoryId() + ")");
        }

        // 3. 处理副图列表持久化格式 (逗号拼接)
        String subImagesJoined = formatSubImages(cmd.subImages());

        Long productId;
        if (cmd.id() == null) {
            // 新增商品
            ShopProduct product = new ShopProduct();
            product.setName(cmd.name().trim());
            product.setSubtitle(cmd.subtitle());
            product.setBrandId(cmd.brandId());
            product.setCategoryId(cmd.categoryId());
            product.setMainImage(cmd.mainImage().trim());
            product.setSubImages(subImagesJoined);
            product.setPrice(cmd.price());
            product.setOriginalPrice(cmd.originalPriceOrDefault());
            product.setStock(cmd.stock());
            product.setStatus(cmd.statusOrDefault());
            product.setDetailHtml(cmd.detailHtml());

            productMapper.insert(product);
            productId = product.getId();
        } else {
            // 更新商品
            ShopProduct existing = productMapper.selectById(cmd.id());
            if (existing == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标商品不存在或已被删除");
            }

            existing.setName(cmd.name().trim());
            existing.setSubtitle(cmd.subtitle());
            existing.setBrandId(cmd.brandId());
            existing.setCategoryId(cmd.categoryId());
            existing.setMainImage(cmd.mainImage().trim());
            existing.setSubImages(subImagesJoined);
            existing.setPrice(cmd.price());
            existing.setOriginalPrice(cmd.originalPriceOrDefault());
            existing.setStock(cmd.stock());
            existing.setStatus(cmd.statusOrDefault());
            existing.setDetailHtml(cmd.detailHtml());

            productMapper.updateById(existing);
            productId = existing.getId();
        }

        // 4. 清除相关 Redis 缓存 (Cache Invalidation)
        evictProductCaches(productId);

        return productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品ID和上下架状态均不能为空");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法状态值，只能为 0 或 1");
        }

        ShopProduct existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在");
        }

        existing.setStatus(status);
        productMapper.updateById(existing);

        evictProductCaches(id);
    }

    @Override
    public PageResult<ProductAdminVO> listAdminProducts(AdminProductQuery query) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.name())) {
            wrapper.like(ShopProduct::getName, query.name().trim());
        }
        if (query.brandId() != null) {
            wrapper.eq(ShopProduct::getBrandId, query.brandId());
        }
        if (query.categoryId() != null) {
            wrapper.eq(ShopProduct::getCategoryId, query.categoryId());
        }
        if (query.status() != null) {
            wrapper.eq(ShopProduct::getStatus, query.status());
        }

        wrapper.orderByDesc(ShopProduct::getId);

        Page<ShopProduct> page = new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault());
        Page<ShopProduct> productPage = productMapper.selectPage(page, wrapper);

        List<ProductAdminVO> vos = toAdminVOList(productPage.getRecords());
        return PageResult.of(productPage.getCurrent(), productPage.getSize(), productPage.getTotal(), vos);
    }

    @Override
    public ProductAdminVO getAdminProductById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品ID不能为空");
        }

        ShopProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品不存在");
        }

        ShopBrand brand = product.getBrandId() != null ? brandMapper.selectById(product.getBrandId()) : null;
        ShopCategory category = product.getCategoryId() != null ? categoryMapper.selectById(product.getCategoryId()) : null;

        return new ProductAdminVO(
                product.getId(),
                product.getBrandId(),
                brand != null ? brand.getName() : "品牌",
                product.getCategoryId(),
                category != null ? category.getName() : "分类",
                product.getName(),
                product.getSubtitle(),
                product.getMainImage(),
                parseSubImages(product.getSubImages()),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus(),
                product.getDetailHtml(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品ID不能为空");
        }

        ShopProduct existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标商品不存在或已被删除");
        }

        productMapper.deleteById(id);
        evictProductCaches(id);
    }

    private void evictProductCaches(Long productId) {
        try {
            // 1. 失效首页聚合橱窗
            redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
            // 2. 失效商品详情缓存
            if (productId != null) {
                redisTemplate.delete(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + productId);
            }
        } catch (Exception e) {
            log.warn("清除商品相关缓存异常: {}", e.getMessage());
        }
    }

    private String formatSubImages(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private List<String> parseSubImages(String rawSubImages) {
        if (!StringUtils.hasText(rawSubImages)) {
            return Collections.emptyList();
        }
        if (rawSubImages.startsWith("[") && rawSubImages.endsWith("]")) {
            try {
                List<String> parsed = objectMapper.readValue(rawSubImages, new TypeReference<List<String>>() {});
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return Arrays.stream(rawSubImages.split(","))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private List<ProductAdminVO> toAdminVOList(List<ShopProduct> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> brandIds = products.stream().map(ShopProduct::getBrandId).filter(b -> b != null && b > 0).collect(Collectors.toSet());
        Set<Long> catIds = products.stream().map(ShopProduct::getCategoryId).filter(c -> c != null && c > 0).collect(Collectors.toSet());

        Map<Long, String> brandNames = brandIds.isEmpty() ? Collections.emptyMap() :
                brandMapper.selectBatchIds(brandIds).stream().collect(Collectors.toMap(ShopBrand::getId, ShopBrand::getName, (a, b) -> a));

        Map<Long, String> catNames = catIds.isEmpty() ? Collections.emptyMap() :
                categoryMapper.selectBatchIds(catIds).stream().collect(Collectors.toMap(ShopCategory::getId, ShopCategory::getName, (a, b) -> a));

        return products.stream().map(p -> new ProductAdminVO(
                p.getId(),
                p.getBrandId(),
                brandNames.getOrDefault(p.getBrandId(), "品牌"),
                p.getCategoryId(),
                catNames.getOrDefault(p.getCategoryId(), "分类"),
                p.getName(),
                p.getSubtitle(),
                p.getMainImage(),
                parseSubImages(p.getSubImages()),
                p.getPrice(),
                p.getOriginalPrice(),
                p.getStock(),
                p.getStatus(),
                p.getDetailHtml(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        )).toList();
    }
}
