package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.cache.TransactionalCacheEvictor;
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
import com.shop.modules.catalog.service.util.CatalogLookupHelper;
import com.shop.modules.catalog.service.util.ProductImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    public ProductAdminServiceImpl(ShopProductMapper productMapper,
                                   ShopBrandMapper brandMapper,
                                   ShopCategoryMapper categoryMapper,
                                   StringRedisTemplate redisTemplate) {
        this.productMapper = productMapper;
        this.brandMapper = brandMapper;
        this.categoryMapper = categoryMapper;
        this.redisTemplate = redisTemplate;
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

        // 3. 构建或获取实体并统一赋值
        boolean isNew = (cmd.id() == null);
        ShopProduct product = isNew ? new ShopProduct() : productMapper.selectById(cmd.id());
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标商品不存在或已被删除");
        }

        product.setName(cmd.name().trim());
        product.setSubtitle(cmd.subtitle());
        product.setBrandId(cmd.brandId());
        product.setCategoryId(cmd.categoryId());
        product.setMainImage(cmd.mainImage().trim());
        product.setSubImages(ProductImageHelper.formatSubImages(cmd.subImages()));
        product.setPrice(cmd.price());
        product.setOriginalPrice(cmd.originalPriceOrDefault());
        product.setStock(cmd.stock());
        product.setStatus(cmd.statusOrDefault());
        product.setDetailHtml(cmd.detailHtml());

        if (isNew) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }

        // 4. 清除相关 Redis 缓存 (Cache Invalidation)
        evictProductCaches(product.getId());

        return product.getId();
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
        AdminProductQuery safeQuery = query != null
                ? query
                : new AdminProductQuery(null, null, null, null, 1, 10);
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(safeQuery.name())) {
            wrapper.like(ShopProduct::getName, safeQuery.name().trim());
        }
        if (safeQuery.brandId() != null) {
            wrapper.eq(ShopProduct::getBrandId, safeQuery.brandId());
        }
        if (safeQuery.categoryId() != null) {
            List<Long> targetCategoryIds = CatalogLookupHelper.collectSelfAndDescendantIds(categoryMapper, safeQuery.categoryId());
            if (targetCategoryIds.size() == 1) {
                wrapper.eq(ShopProduct::getCategoryId, targetCategoryIds.get(0));
            } else {
                wrapper.in(ShopProduct::getCategoryId, targetCategoryIds);
            }
        }
        if (safeQuery.status() != null) {
            wrapper.eq(ShopProduct::getStatus, safeQuery.status());
        }

        wrapper.orderByDesc(ShopProduct::getId);

        Page<ShopProduct> page = new Page<>(safeQuery.pageNumOrDefault(), safeQuery.pageSizeOrDefault());
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
                ProductImageHelper.parseSubImages(product.getSubImages()),
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
        TransactionalCacheEvictor.evictNowAndAfterCommit(() -> doEvictProductCaches(productId));
    }

    private void doEvictProductCaches(Long productId) {
        try {
            redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
            if (productId != null) {
                redisTemplate.delete(ProductCatalogServiceImpl.PRODUCT_DETAIL_CACHE_PREFIX + productId);
            }
        } catch (Exception e) {
            log.warn("清除商品相关缓存异常: {}", e.getMessage());
        }
    }

    private List<ProductAdminVO> toAdminVOList(List<ShopProduct> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> brandIds = products.stream().map(ShopProduct::getBrandId).filter(b -> b != null && b > 0).collect(Collectors.toSet());
        Set<Long> catIds = products.stream().map(ShopProduct::getCategoryId).filter(c -> c != null && c > 0).collect(Collectors.toSet());

        Map<Long, String> brandNames = CatalogLookupHelper.loadBrandNames(brandMapper, brandIds);
        Map<Long, String> catNames = CatalogLookupHelper.loadCategoryNames(categoryMapper, catIds);

        return products.stream().map(p -> new ProductAdminVO(
                p.getId(),
                p.getBrandId(),
                brandNames.getOrDefault(p.getBrandId(), "品牌"),
                p.getCategoryId(),
                catNames.getOrDefault(p.getCategoryId(), "分类"),
                p.getName(),
                p.getSubtitle(),
                p.getMainImage(),
                ProductImageHelper.parseSubImages(p.getSubImages()),
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
