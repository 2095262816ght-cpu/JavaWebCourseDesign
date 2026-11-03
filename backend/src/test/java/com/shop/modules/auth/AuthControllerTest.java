package com.shop.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("测试公开注册与登录流程 (HTTP API)")
    void testRegisterAndLoginApi() throws Exception {
        String testUser = "api_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterCommand registerCmd = new RegisterCommand(testUser, "12345678", "API测试买家", null);

        // 1. 发起注册
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.username", is(testUser)))
                .andExpect(jsonPath("$.data.role", is("ROLE_CUSTOMER")));

        // 2. 发起登录
        LoginCommand loginCmd = new LoginCommand(testUser, "12345678");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.tokenValue").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username", is(testUser)));
    }

    @Test
    @DisplayName("测试未授权直接访问管理端接口被拦截 (HTTP 401)")
    void testAnonymousAccessAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)))
                .andExpect(jsonPath("$.message", containsString("请先登录")));
    }

    @Test
    @DisplayName("测试普通顾客Token访问管理端被拦截 (HTTP 403)")
    void testCustomerAccessAdminForbidden() throws Exception {
        // 先以 customer 身份登录获取 token
        LoginCommand customerCmd = new LoginCommand("customer", "123456");
        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerCmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseStr).path("data").path("tokenValue").asText();

        // 携带普通顾客 token 尝试访问管理员专属接口
        mockMvc.perform(get("/api/admin/users")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("权限不足")));
    }

    @Test
    @DisplayName("测试超级管理员Token正常访问管理端接口 (HTTP 200)")
    void testAdminAccessAdminSuccess() throws Exception {
        // 以 admin 身份登录
        LoginCommand adminCmd = new LoginCommand("admin", "admin123");
        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminCmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseStr).path("data").path("tokenValue").asText();

        // 携带管理员 token 访问
        mockMvc.perform(get("/api/admin/users")
                        .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
