package com.shop.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import com.shop.common.storage.StorageProperties;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 启用状态缓存时长：避免每个受保护请求都查一次用户表，同时保证禁用最长 30 秒内生效。
    // 禁用账号时 UserServiceImpl 会立即注销其会话，因此实际生效通常是即时的。
    private static final long ACTIVE_CACHE_TTL_MS = 30_000L;
    private static final int ACTIVE_CACHE_CLEANUP_THRESHOLD = 5_000;

    private final StorageProperties storageProperties;
    private final SysUserMapper sysUserMapper;
    private final Map<Long, Long> activeUserCache = new ConcurrentHashMap<>();

    public WebMvcConfig(StorageProperties storageProperties, SysUserMapper sysUserMapper) {
        this.storageProperties = storageProperties;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * Map public URL path to local uploads folder
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadAbsolutePath = Paths.get(storageProperties.getLocalDir()).toAbsolutePath().normalize().toUri().toString();
        if (!uploadAbsolutePath.endsWith("/")) {
            uploadAbsolutePath += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadAbsolutePath);
    }

    /**
     * Cross-Origin Resource Sharing (CORS) for Vue frontend dev server
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Sa-Token Interceptor for authentication & role protection
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // Admin management routes require ROLE_ADMIN & active user status
            SaRouter.match("/api/admin/**", r -> {
                checkActiveUser();
                StpUtil.checkRole("ROLE_ADMIN");
            });
            // Authenticated user actions
            SaRouter.match("/api/auth/profile", r -> checkActiveUser());
            SaRouter.match("/api/auth/logout", r -> StpUtil.checkLogin());
            SaRouter.match("/api/cart/**", r -> checkActiveUser());
            SaRouter.match("/api/orders/**", r -> checkActiveUser());
            SaRouter.match("/api/common/upload", r -> checkActiveUser());
        })).addPathPatterns("/**")
           .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/register",
                "/api/portal/**",
                "/uploads/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico"
           );
    }

    private void checkActiveUser() {
        StpUtil.checkLogin();
        long userId = StpUtil.getLoginIdAsLong();
        long now = System.currentTimeMillis();

        Long activeUntil = activeUserCache.get(userId);
        if (activeUntil != null && activeUntil > now) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在或登录已过期");
        }
        if (!user.isActive()) {
            // 不缓存禁用结果：管理员重新启用后无需等待缓存过期
            activeUserCache.remove(userId);
            StpUtil.logout();
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "该账号已被禁用，请联系平台管理员");
        }

        if (activeUserCache.size() > ACTIVE_CACHE_CLEANUP_THRESHOLD) {
            activeUserCache.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        activeUserCache.put(userId, now + ACTIVE_CACHE_TTL_MS);
    }
}
