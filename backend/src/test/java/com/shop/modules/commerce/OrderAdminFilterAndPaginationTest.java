package com.shop.modules.commerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.DeliveryAddress;
import com.shop.modules.commerce.dto.OrderCheckoutResult;
import com.shop.modules.commerce.service.CartService;
import com.shop.modules.commerce.service.OrderCheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderAdminFilterAndPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setupTokens() throws Exception {
        String adminResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginCommand("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminResp).path("data").path("tokenValue").asText();

        String custResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginCommand("customer", "123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        customerToken = objectMapper.readTree(custResp).path("data").path("tokenValue").asText();
    }

    @Test
    @DisplayName("测试管理端与顾客端订单分页钳制 [1,100] 及状态小写归一化筛选")
    void testOrderPaginationAndLowercaseStatusFilter() throws Exception {
        cartService.clearCart(2L);
        cartService.addItem(2L, new AddCartCommand(1L, 1));
        OrderCheckoutResult created = orderCheckoutService.checkout(
                2L,
                new CheckoutCommand(new DeliveryAddress("分页测试人", "13900139000", "上海市浦东新区测试路1号"))
        );

        mockMvc.perform(get("/api/admin/orders")
                        .header("satoken", adminToken)
                        .param("orderNo", created.getOrderNo())
                        .param("status", " unpaid ")
                        .param("pageNum", "0")
                        .param("pageSize", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.current", is(1)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].orderNo", is(created.getOrderNo())));

        mockMvc.perform(get("/api/orders/my")
                        .header("satoken", customerToken)
                        .param("status", "unpaid")
                        .param("pageNum", "-2")
                        .param("pageSize", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.current", is(1)))
                .andExpect(jsonPath("$.data.size", is(100)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));
    }
}
