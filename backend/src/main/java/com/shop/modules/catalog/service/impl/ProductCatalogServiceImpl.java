package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.BrandVO;
import com.shop.modules.catalog.dto.CarouselSlideVO;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.dto.HomeShowcaseVO;
import com.shop.modules.catalog.dto.ProductCardVO;
import com.shop.modules.catalog.dto.ProductDetailVO;
import com.shop.modules.catalog.dto.ProductQueryCommand;
import com.shop.modules.catalog.entity.ShopBrand;
import com.shop.modules.catalog.entity.ShopCategory;
import com.shop.modules.catalog.entity.ShopProduct;
import com.shop.modules.catalog.mapper.ShopBrandMapper;
import com.shop.modules.catalog.mapper.ShopCategoryMapper;
import com.shop.modules.catalog.mapper.ShopProductMapper;
import com.shop.modules.catalog.service.BrandService;
import com.shop.modules.catalog.service.CategoryService;
import com.shop.modules.catalog.service.ProductCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogServiceImpl.class);

    public static final String HOME_SHOWCASE_CACHE_KEY = "catalog:showcase:home";
    public static final String PRODUCT_DETAIL_CACHE_PREFIX = "catalog:product:detail:";
    private static final Duration SHOWCASE_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);

    private final ShopProductMapper productMapper;
    private final ShopBrandMapper brandMapper;
    private final ShopCategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCatalogServiceImpl(ShopProductMapper productMapper,
                                     ShopBrandMapper brandMapper,
                                     ShopCategoryMapper categoryMapper,
                                     CategoryService categoryService,
                                     BrandService brandService,
                                     StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.brandMapper = brandMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public HomeShowcaseVO getHomeShowcase() {
        // 1. 尝试读取 Redis 缓存 (消除瀑布流请求加速响应)
        try {
            String cachedJson = redisTemplate.opsForValue().get(HOME_SHOWCASE_CACHE_KEY);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, HomeShowcaseVO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取首页聚合橱窗缓存异常，降级直查数据库: {}", e.getMessage());
        }

        // 2. 聚合分类层级树
        List<CategoryNodeVO> categories = categoryService.getCategoryTree();

        // 3. 聚合甄选品牌列表 (前 8 个品牌)
        List<BrandVO> featuredBrands = brandService.getFeaturedBrands(8);

        // 4. 聚合热卖推荐商品 (在售、按价格或排序降序取前 8 条)
        LambdaQueryWrapper<ShopProduct> hotWrapper = new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getStatus, 1)
                .orderByDesc(ShopProduct::getPrice)
                .last("LIMIT 8");
        List<ShopProduct> hotList = productMapper.selectList(hotWrapper);
        List<ProductCardVO> hotProducts = toCardVOList(hotList);

        // 5. 聚合最新上架商品 (在售、按创建时间倒序取前 8 条)
        LambdaQueryWrapper<ShopProduct> newWrapper = new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getStatus, 1)
                .orderByDesc(ShopProduct::getCreatedAt)
                .orderByDesc(ShopProduct::getId)
                .last("LIMIT 8");
        List<ShopProduct> newList = productMapper.selectList(newWrapper);
        List<ProductCardVO> newProducts = toCardVOList(newList);

        // 6. 聚合焦点轮播图 (优先使用热卖商品的真实封面生成沉浸式轮播)
        List<CarouselSlideVO> carousels = buildCarousels(hotProducts);

        HomeShowcaseVO showcase = new HomeShowcaseVO(
                carousels,
                featuredBrands,
                categories,
                hotProducts,
                newProducts
        );

        // 7. 异步/安全写回 Redis
        try {
            String json = objectMapper.writeValueAsString(showcase);
            redisTemplate.opsForValue().set(HOME_SHOWCASE_CACHE_KEY, json, SHOWCASE_CACHE_TTL);
        } catch (Exception e) {
            log.warn("首页聚合橱窗数据写入 Redis 失败: {}", e.getMessage());
        }

        return showcase;
    }

    @Override
    public PageResult<ProductCardVO> searchProducts(ProductQueryCommand cmd) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopProduct::getStatus, 1);

        // 关键字模糊搜索 (标题或副标题)
        if (StringUtils.hasText(cmd.keyword())) {
            String kw = cmd.keyword().trim();
            wrapper.and(w -> w.like(ShopProduct::getName, kw).or().like(ShopProduct::getSubtitle, kw));
        }

        // 品牌过滤
        if (cmd.brandId() != null) {
            wrapper.eq(ShopProduct::getBrandId, cmd.brandId());
        }

        // 分类过滤 (支持父级分类查询：自动包含该父分类及其所有子分类商品)
        if (cmd.categoryId() != null) {
            List<Long> targetCategoryIds = resolveCategoryHierarchy(cmd.categoryId());
            if (targetCategoryIds.size() == 1) {
                wrapper.eq(ShopProduct::getCategoryId, targetCategoryIds.get(0));
            } else {
                wrapper.in(ShopProduct::getCategoryId, targetCategoryIds);
            }
        }

        // 价格区间过滤
        if (cmd.minPrice() != null) {
            wrapper.ge(ShopProduct::getPrice, cmd.minPrice());
        }
        if (cmd.maxPrice() != null) {
            wrapper.le(ShopProduct::getPrice, cmd.maxPrice());
        }

        // 动态排序
        if ("price_asc".equalsIgnoreCase(cmd.sortBy())) {
            wrapper.orderByAsc(ShopProduct::getPrice).orderByDesc(ShopProduct::getId);
        } else if ("price_desc".equalsIgnoreCase(cmd.sortBy())) {
            wrapper.orderByDesc(ShopProduct::getPrice).orderByDesc(ShopProduct::getId);
        } else if ("newest".equalsIgnoreCase(cmd.sortBy())) {
            wrapper.orderByDesc(ShopProduct::getCreatedAt).orderByDesc(ShopProduct::getId);
        } else {
            wrapper.orderByDesc(ShopProduct::getId);
        }

        // 分页执行
        Page<ShopProduct> page = new Page<>(cmd.pageNumOrDefault(), cmd.pageSizeOrDefault());
        Page<ShopProduct> productPage = productMapper.selectPage(page, wrapper);

        List<ProductCardVO> cardVOs = toCardVOList(productPage.getRecords());
        return PageResult.of(productPage.getCurrent(), productPage.getSize(), productPage.getTotal(), cardVOs);
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "商品ID不能为空");
        }

        String cacheKey = PRODUCT_DETAIL_CACHE_PREFIX + id;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, ProductDetailVO.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取商品详情缓存异常，降级直查数据库: {}", e.getMessage());
        }

        ShopProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "商品未找到或已被移除");
        }

        // 解析所属品牌与分类
        ShopBrand brand = product.getBrandId() != null ? brandMapper.selectById(product.getBrandId()) : null;
        ShopCategory category = product.getCategoryId() != null ? categoryMapper.selectById(product.getCategoryId()) : null;

        List<String> subImages = parseSubImages(product.getMainImage(), product.getSubImages());

        ProductDetailVO detail = new ProductDetailVO(
                product.getId(),
                product.getBrandId(),
                brand != null ? brand.getName() : "品牌",
                brand != null ? brand.getLogo() : null,
                product.getCategoryId(),
                category != null ? category.getName() : "分类",
                product.getName(),
                product.getSubtitle(),
                product.getMainImage(),
                subImages,
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus(),
                product.getDetailHtml(),
                product.getCreatedAt()
        );

        try {
            String json = objectMapper.writeValueAsString(detail);
            redisTemplate.opsForValue().set(cacheKey, json, DETAIL_CACHE_TTL);
        } catch (Exception e) {
            log.warn("商品详情写入 Redis 缓存失败: {}", e.getMessage());
        }

        return detail;
    }

    private List<Long> resolveCategoryHierarchy(Long rootCategoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(rootCategoryId);

        LambdaQueryWrapper<ShopCategory> childWrapper = new LambdaQueryWrapper<ShopCategory>()
                .eq(ShopCategory::getParentId, rootCategoryId);
        List<ShopCategory> children = categoryMapper.selectList(childWrapper);
        if (children != null && !children.isEmpty()) {
            for (ShopCategory child : children) {
                ids.add(child.getId());
            }
        }
        return ids;
    }

    private List<CarouselSlideVO> buildCarousels(List<ProductCardVO> products) {
        List<CarouselSlideVO> slides = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            int limit = Math.min(products.size(), 4);
            for (int i = 0; i < limit; i++) {
                ProductCardVO p = products.get(i);
                slides.add(new CarouselSlideVO(
                        (long) (i + 1),
                        p.name(),
                        StringUtils.hasText(p.subtitle()) ? p.subtitle() : "官方正品 · 尊享特惠",
                        p.mainImage(),
                        "/product/" + p.id(),
                        p.id()
                ));
            }
        } else {
            // 兜底高质感轮播
            slides.add(new CarouselSlideVO(
                    1L,
                    "旗舰新品 尊享盛宴",
                    "严选全球尖货，品质生活触手可及",
                    "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=1600&h=600&fit=crop",
                    "/products",
                    null
            ));
        }
        return slides;
    }

    private List<ProductCardVO> toCardVOList(List<ShopProduct> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> brandIds = products.stream().map(ShopProduct::getBrandId).filter(b -> b != null && b > 0).collect(Collectors.toSet());
        Set<Long> catIds = products.stream().map(ShopProduct::getCategoryId).filter(c -> c != null && c > 0).collect(Collectors.toSet());

        Map<Long, String> brandNames = brandIds.isEmpty() ? Collections.emptyMap() :
                brandMapper.selectBatchIds(brandIds).stream().collect(Collectors.toMap(ShopBrand::getId, ShopBrand::getName, (a, b) -> a));

        Map<Long, String> catNames = catIds.isEmpty() ? Collections.emptyMap() :
                categoryMapper.selectBatchIds(catIds).stream().collect(Collectors.toMap(ShopCategory::getId, ShopCategory::getName, (a, b) -> a));

        return products.stream().map(p -> new ProductCardVO(
                p.getId(),
                p.getName(),
                p.getSubtitle(),
                p.getMainImage(),
                p.getPrice(),
                p.getOriginalPrice(),
                p.getBrandId(),
                brandNames.getOrDefault(p.getBrandId(), "品牌"),
                p.getCategoryId(),
                catNames.getOrDefault(p.getCategoryId(), "分类"),
                p.getStock(),
                p.getStatus()
        )).toList();
    }

    private List<String> parseSubImages(String mainImage, String rawSubImages) {
        List<String> list = new ArrayList<>();
        if (StringUtils.hasText(rawSubImages)) {
            if (rawSubImages.startsWith("[") && rawSubImages.endsWith("]")) {
                try {
                    List<String> parsed = objectMapper.readValue(rawSubImages, new TypeReference<List<String>>() {});
                    if (parsed != null) {
                        list.addAll(parsed);
                    }
                } catch (Exception ignored) {
                }
            } else {
                String[] parts = rawSubImages.split(",");
                for (String part : parts) {
                    if (StringUtils.hasText(part)) {
                        list.add(part.trim());
                    }
                }
            }
        }

        // 如果副图为空，至少保留主图供前台画廊展示
        if (list.isEmpty() && StringUtils.hasText(mainImage)) {
            list.add(mainImage);
        }
        return list;
    }
}
