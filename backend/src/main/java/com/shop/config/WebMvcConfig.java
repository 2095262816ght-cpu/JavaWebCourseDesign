package com.shop.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.storage.StorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public WebMvcConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
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
            // Admin management routes require ROLE_ADMIN
            SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("ROLE_ADMIN"));
            // User cart & order routes require authentication
            SaRouter.match("/api/cart/**", r -> StpUtil.checkLogin());
            SaRouter.match("/api/orders/**", r -> StpUtil.checkLogin());
        })).addPathPatterns("/**")
           .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/register",
                "/api/portal/**",
                "/api/common/**",
                "/uploads/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico"
           );
    }
}
