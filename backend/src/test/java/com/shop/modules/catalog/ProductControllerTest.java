package com.shop.modules.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.catalog.dto.ProductCommand;
import com.shop.modules.catalog.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String adminToken;

    @AfterEach
    void cleanupShowcaseCache() {
        redisTemplate.delete(ProductCatalogServiceImpl.HOME_SHOWCASE_CACHE_KEY);
    }

    @BeforeEach
    void setupAdminToken() throws Exception {
        LoginCommand adminCmd = new LoginCommand("admin", "admin123");
        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminCmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = objectMapper.readTree(responseStr).path("data").path("tokenValue").asText();
    }

    @Test
    @DisplayName("测试前台公开端点：首页聚合橱窗、商品检索与详情")
    void testPortalProductEndpoints() throws Exception {
        // 1. 首页聚合橱窗
        mockMvc.perform(get("/api/portal/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.carousels").isArray())
                .andExpect(jsonPath("$.data.featuredBrands").isArray())
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.hotProducts").isArray())
                .andExpect(jsonPath("$.data.newProducts").isArray());

        // 2. 多条件商品检索
        mockMvc.perform(get("/api/portal/products")
                        .param("keyword", "iPhone")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        // 3. 商品详情
        mockMvc.perform(get("/api/portal/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", notNullValue()))
                .andExpect(jsonPath("$.data.brandName", notNullValue()));
    }

    @Test
    @DisplayName("测试后台安全防护：未携带管理员Token时拦截")
    void testAdminSecurityBoundary() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)));
    }

    @Test
    @DisplayName("测试管理后台商品全生命周期 CRUD 与状态变更")
    void testAdminProductLifecycle() throws Exception {
        // 1. 新建商品
        ProductCommand createCmd = new ProductCommand(
                null,
                "自动化测试录入商品",
                "副标题描述",
                1L, // Apple
                5L, // 智能手机
                "https://images.unsplash.com/test-main.jpg",
                List.of("https://images.unsplash.com/test-sub.jpg"),
                new BigDecimal("3999.00"),
                new BigDecimal("4299.00"),
                50,
                1,
                "<p>详细规格</p>"
        );

        String createResp = mockMvc.perform(post("/api/admin/products")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        long createdId = objectMapper.readTree(createResp).path("data").asLong();

        // 2. 获取商品后台详情
        mockMvc.perform(get("/api/admin/products/" + createdId)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.name", is("自动化测试录入商品")))
                .andExpect(jsonPath("$.data.price", is(3999.00)));

        // 3. 修改商品价格与库存
        ProductCommand updateCmd = new ProductCommand(
                createdId,
                "自动化测试录入商品-修改版",
                "副标题描述",
                1L,
                5L,
                "https://images.unsplash.com/test-main.jpg",
                List.of("https://images.unsplash.com/test-sub.jpg"),
                new BigDecimal("3699.00"),
                new BigDecimal("4299.00"),
                88,
                1,
                "<p>详细规格-更新</p>"
        );

        mockMvc.perform(put("/api/admin/products/" + createdId)
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 4. 下架商品 (PATCH)
        mockMvc.perform(patch("/api/admin/products/" + createdId + "/status")
                        .param("status", "0")
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 5. 分页查询后台列表
        mockMvc.perform(get("/api/admin/products")
                        .param("name", "修改版")
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        // 6. 删除商品
        mockMvc.perform(delete("/api/admin/products/" + createdId)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));
    }

    @Test
    @DisplayName("测试商品前后台分页越界钳制 [1,100] 与非法状态值拦截")
    void testProductPaginationClampingAndStatusValidation() throws Exception {
        mockMvc.perform(get("/api/portal/products")
                        .param("pageNum", "0")
                        .param("pageSize", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.current", is(1)))
                .andExpect(jsonPath("$.data.size", is(100)));

        mockMvc.perform(get("/api/admin/products")
                        .header("satoken", adminToken)
                        .param("pageNum", "-1")
                        .param("pageSize", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.current", is(1)))
                .andExpect(jsonPath("$.data.size", is(100)));

        ProductCommand invalidProd = new ProductCommand(
                null, "非法状态测试商品", "副标题", 1L, 5L,
                "https://images.unsplash.com/test-main.jpg", List.of(),
                new BigDecimal("199.00"), new BigDecimal("299.00"), 10, 99, "<p>详情</p>"
        );
        mockMvc.perform(post("/api/admin/products")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));
    }
}
