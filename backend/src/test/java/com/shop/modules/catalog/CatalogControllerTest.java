package com.shop.modules.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.catalog.dto.BrandCommand;
import com.shop.modules.catalog.dto.CategoryCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

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
    @DisplayName("测试公开端点访问：分类树与推荐品牌无需鉴权")
    void testPublicEndpoints() throws Exception {
        // 1. 公开获取分类树
        mockMvc.perform(get("/api/portal/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isArray());

        // 2. 公开获取推荐品牌
        mockMvc.perform(get("/api/portal/brands/featured?limit=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("测试分类管理端点完整 CRUD 流程")
    void testAdminCategoryCrud() throws Exception {
        // 1. 管理员查询分类树
        mockMvc.perform(get("/api/admin/categories/tree")
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isArray());

        // 2. 创建新的一级分类
        CategoryCommand createCmd = new CategoryCommand(
                null,
                0L,
                "智能家居测试",
                "el-icon-house",
                99,
                1
        );
        mockMvc.perform(post("/api/admin/categories")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));
    }

    @Test
    @DisplayName("测试品牌管理端点分页查询与新增")
    void testAdminBrandEndpoints() throws Exception {
        // 1. 分页检索
        mockMvc.perform(get("/api/admin/brands")
                        .header("satoken", adminToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        // 2. 新增品牌
        BrandCommand brandCmd = new BrandCommand(
                null,
                "Logitech",
                "https://images.unsplash.com/photo-logi",
                "瑞士计算机外设与办公软件制造商",
                15,
                1
        );
        mockMvc.perform(post("/api/admin/brands")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));
    }

    @Test
    @DisplayName("测试匿名直接调用管理端接口被拦截")
    void testAnonymousForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)))
                .andExpect(jsonPath("$.message", containsString("请先登录")));

        mockMvc.perform(get("/api/admin/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)))
                .andExpect(jsonPath("$.message", containsString("请先登录")));
    }
}
