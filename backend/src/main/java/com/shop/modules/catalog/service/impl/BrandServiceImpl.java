package com.shop.modules.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.BrandCommand;
import com.shop.modules.catalog.dto.BrandQuery;
import com.shop.modules.catalog.dto.BrandVO;
import com.shop.modules.catalog.entity.ShopBrand;
import com.shop.modules.catalog.mapper.ShopBrandMapper;
import com.shop.modules.catalog.service.BrandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class BrandServiceImpl implements BrandService {

    private static final Logger log = LoggerFactory.getLogger(BrandServiceImpl.class);
    public static final String BRAND_FEATURED_CACHE_PREFIX = "catalog:brand:featured:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final ShopBrandMapper brandMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BrandServiceImpl(ShopBrandMapper brandMapper,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper) {
        this.brandMapper = brandMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<BrandVO> getFeaturedBrands(int limit) {
        int safeLimit = limit <= 0 ? 6 : Math.min(limit, 50);
        String cacheKey = BRAND_FEATURED_CACHE_PREFIX + safeLimit;

        // 1. 读取 Redis 缓存
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, new TypeReference<List<BrandVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取推荐品牌缓存异常，降级直查数据库: {}", e.getMessage());
        }

        // 2. 缓存未命中，回源 MySQL 查询启用品牌
        Page<ShopBrand> pageParam = new Page<>(1, safeLimit);
        LambdaQueryWrapper<ShopBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopBrand::getStatus, 1)
                .orderByAsc(ShopBrand::getSort)
                .orderByAsc(ShopBrand::getId);

        Page<ShopBrand> brandPage = brandMapper.selectPage(pageParam, wrapper);
        List<BrandVO> result = brandPage.getRecords().stream()
                .map(this::toVO)
                .toList();

        // 3. 回写 Redis
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis 写入推荐品牌缓存异常: {}", e.getMessage());
        }

        return result;
    }

    public static final String BRAND_ALL_CACHE_KEY = "catalog:brand:all";
    public static final String BRAND_LIST_CACHE_PREFIX = "catalog:brand:list:";

    @Override
    public PageResult<BrandVO> listBrands(BrandQuery query) {
        int current = query != null ? query.getPageOrDefault() : 1;
        int size = query != null ? query.getSizeOrDefault() : 10;
        String kw = (query != null && StringUtils.hasText(query.keyword())) ? query.keyword().trim() : "";
        Integer status = query != null ? query.status() : null;

        String cacheKey = (kw.isEmpty() && status == null && current == 1 && size == 10)
                ? BRAND_ALL_CACHE_KEY
                : BRAND_LIST_CACHE_PREFIX + current + ":" + size + ":" + kw + ":" + status;

        // 1. 读取 Redis 缓存 (Cache-Aside 模式)
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, new TypeReference<PageResult<BrandVO>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取品牌列表缓存异常，降级直查数据库: {}", e.getMessage());
        }

        // 2. 缓存未命中，回源 MySQL 查询
        Page<ShopBrand> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<ShopBrand> wrapper = new LambdaQueryWrapper<>();

        if (!kw.isEmpty()) {
            wrapper.and(w -> w.like(ShopBrand::getName, kw).or().like(ShopBrand::getDescription, kw));
        }

        if (status != null) {
            wrapper.eq(ShopBrand::getStatus, status);
        }

        wrapper.orderByAsc(ShopBrand::getSort)
                .orderByAsc(ShopBrand::getId);

        Page<ShopBrand> page = brandMapper.selectPage(pageParam, wrapper);

        List<BrandVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        PageResult<BrandVO> result = PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), voList);

        // 3. 回写 Redis
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis 写入品牌列表缓存异常: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public BrandVO getBrandById(Long id) {
        ShopBrand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标品牌不存在或已被删除");
        }
        return toVO(brand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBrand(BrandCommand cmd) {
        if (cmd.id() != null) {
            // 更新品牌
            ShopBrand existing = brandMapper.selectById(cmd.id());
            if (existing == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标品牌不存在或已被删除");
            }
            existing.setName(cmd.name());
            existing.setOrigin(cmd.origin());
            existing.setLogo(cmd.logo());
            existing.setDescription(cmd.description());
            existing.setSort(cmd.sortOrDefault());
            existing.setStatus(cmd.statusOrDefault());
            brandMapper.updateById(existing);
        } else {
            // 新增品牌
            ShopBrand brand = new ShopBrand();
            brand.setName(cmd.name());
            brand.setOrigin(cmd.origin());
            brand.setLogo(cmd.logo());
            brand.setDescription(cmd.description());
            brand.setSort(cmd.sortOrDefault());
            brand.setStatus(cmd.statusOrDefault());
            brandMapper.insert(brand);
        }

        // 清理所有品牌相关缓存
        evictBrandCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBrand(Long id) {
        ShopBrand existing = brandMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "目标品牌不存在或已被删除");
        }
        brandMapper.deleteById(id);

        // 清理所有品牌相关缓存
        evictBrandCache();
    }

    private void evictBrandCache() {
        try {
            Set<String> keys = redisTemplate.keys("catalog:brand:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("主动清除品牌相关缓存失败: {}", e.getMessage());
        }
    }

    private BrandVO toVO(ShopBrand b) {
        return new BrandVO(
                b.getId(),
                b.getName(),
                b.getOrigin(),
                b.getLogo(),
                b.getDescription(),
                b.getSort(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
