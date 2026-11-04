package com.shop.modules.commerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.UpdateCartItemCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommerceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setupTokens() throws Exception {
        // 获取普通顾客 Token (customer / 123456)
        LoginCommand customerCmd = new LoginCommand("customer", "123456");
        String customerRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerCmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        customerToken = objectMapper.readTree(customerRes).path("data").path("tokenValue").asText();

        // 获取管理员 Token (admin / admin123)
        LoginCommand adminCmd = new LoginCommand("admin", "admin123");
        String adminRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminCmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminRes).path("data").path("tokenValue").asText();
    }

    @Test
    @DisplayName("测试购物车接口调用全流程：加购、列表、修改、删除与清空")
    void testCartEndpoints() throws Exception {
        // 1. 清空当前用户原有购物车
        mockMvc.perform(delete("/api/cart")
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 2. 加购商品 1 (iPhone 16 Pro Max)
        AddCartCommand addCmd = new AddCartCommand(1L, 2);
        mockMvc.perform(post("/api/cart/items")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 3. 获取购物车明细
        String cartRes = mockMvc.perform(get("/api/cart")
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.totalItems", is(1)))
                .andExpect(jsonPath("$.data.checkedCount", is(2)))
                .andReturn().getResponse().getContentAsString();

        long cartItemId = objectMapper.readTree(cartRes).path("data").path("items").get(0).path("id").asLong();

        // 4. 更新购物车项数量为 1
        UpdateCartItemCommand updateCmd = new UpdateCartItemCommand(1, true);
        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 5. 移除该购物车项
        mockMvc.perform(delete("/api/cart/items/" + cartItemId)
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));
    }

    @Test
    @DisplayName("测试订单全流程端点：下单、支付、顾客订单查询、管理员订单检索与标记发货")
    void testOrderCommerceEndpoints() throws Exception {
        // 1. 加购一件商品 6 (Nike 跑鞋)
        mockMvc.perform(delete("/api/cart")
                        .header("satoken", customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartCommand(6L, 1))))
                .andExpect(status().isOk());

        // 2. 顾客结算下单
        CheckoutCommand checkoutCmd = new CheckoutCommand("张同学", "13911112222", "北京市海淀区学院路10号");
        String checkoutRes = mockMvc.perform(post("/api/orders/checkout")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(checkoutRes).path("data").path("orderId").asLong();
        String orderNo = objectMapper.readTree(checkoutRes).path("data").path("orderNo").asText();

        // 3. 模拟顾客支付
        mockMvc.perform(post("/api/orders/" + orderNo + "/pay")
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 4. 查询顾客个人订单列表与详情
        mockMvc.perform(get("/api/orders/my")
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.orderNo", is(orderNo)))
                .andExpect(jsonPath("$.data.status", is("PAID")));

        // 5. 管理员查询后台订单列表
        mockMvc.perform(get("/api/admin/orders")
                        .header("satoken", adminToken)
                        .param("orderNo", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        // 6. 管理员标记发货
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/ship")
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        // 7. 再次查看详情，状态已流转为 SHIPPED
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("satoken", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SHIPPED")));
    }
}
