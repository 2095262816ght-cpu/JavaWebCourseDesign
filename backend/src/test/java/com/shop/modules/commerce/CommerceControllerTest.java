package com.shop.modules.commerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import com.shop.modules.commerce.dto.AddCartCommand;
import com.shop.modules.commerce.dto.CheckoutCommand;
import com.shop.modules.commerce.dto.UpdateCartItemCommand;
import com.shop.modules.system.dto.UserStatusUpdateDTO;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommerceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

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

        // 5. 管理员查询后台订单列表与任意订单详情
        mockMvc.perform(get("/api/admin/orders")
                        .header("satoken", adminToken)
                        .param("orderNo", orderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.records").isArray());

        // 管理员调用前台接口查看他人订单仍应被拒绝 (403)
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)));

        // 管理员调用管理端详情接口可以正常查看全平台顾客订单快照
        mockMvc.perform(get("/api/admin/orders/" + orderId)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.orderNo", is(orderNo)))
                .andExpect(jsonPath("$.data.items").isArray());

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

    @Test
    @DisplayName("测试顾客订单历史服务端状态筛选分页")
    void testMyOrdersServerSideStatusFilter() throws Exception {
        mockMvc.perform(delete("/api/cart").header("satoken", customerToken)).andExpect(status().isOk());

        // 创建一笔 UNPAID 订单
        mockMvc.perform(post("/api/cart/items")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartCommand(6L, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/checkout")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutCommand("李四", "13922223333", "上海市徐汇区漕河泾1号"))))
                .andExpect(status().isOk());

        // 创建第二笔并支付成 PAID 订单
        mockMvc.perform(post("/api/cart/items")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartCommand(1L, 1))))
                .andExpect(status().isOk());

        String paidCheckoutRes = mockMvc.perform(post("/api/orders/checkout")
                        .header("satoken", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutCommand("李四", "13922223333", "上海市徐汇区漕河泾1号"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paidOrderNo = objectMapper.readTree(paidCheckoutRes).path("data").path("orderNo").asText();

        mockMvc.perform(post("/api/orders/" + paidOrderNo + "/pay")
                        .header("satoken", customerToken))
                .andExpect(status().isOk());

        // 验证按状态服务端筛选分页
        mockMvc.perform(get("/api/orders/my")
                        .header("satoken", customerToken)
                        .param("status", "UNPAID")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[*].status", everyItem(is("UNPAID"))));

        mockMvc.perform(get("/api/orders/my")
                        .header("satoken", customerToken)
                        .param("status", "PAID")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[*].status", everyItem(is("PAID"))));
    }

    @Test
    @DisplayName("测试被禁用账号携带既有 JWT 访问受保护接口应立即拒绝")
    void testDisabledUserJwtRejected() throws Exception {
        // 注册一个临时测试顾客并登录获取独立 Token，避免影响公共种子账号在 Redis 中的会话
        String tempUsername = "disabled_test_" + System.nanoTime();
        String regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterCommand(tempUsername, "123456", "禁用测试用户", null)
                        )))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long tempUserId = objectMapper.readTree(regRes).path("data").path("id").asLong();

        String loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginCommand(tempUsername, "123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tempToken = objectMapper.readTree(loginRes).path("data").path("tokenValue").asText();

        // 场景 A：即使未经过 StpUtil.logout 踢下线，只要数据库中账号状态已为 0，拦截器应返回 403 拒绝访问
        SysUser user = sysUserMapper.selectById(tempUserId);
        user.setStatus(0);
        sysUserMapper.updateById(user);

        mockMvc.perform(get("/api/cart")
                        .header("satoken", tempToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("已被禁用")));

        // 恢复启用并重新登录后，通过管理端禁用接口强制踢下线应返回 401
        user.setStatus(1);
        sysUserMapper.updateById(user);
        String reloginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginCommand(tempUsername, "123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String kickedToken = objectMapper.readTree(reloginRes).path("data").path("tokenValue").asText();

        mockMvc.perform(put("/api/admin/users/" + tempUserId + "/status")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserStatusUpdateDTO(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(get("/api/orders/my")
                        .header("satoken", kickedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)));
    }

    @Test
    @DisplayName("测试文件上传鉴权与危险扩展名拦截")
    void testUploadSecurityGuards() throws Exception {
        MockMultipartFile htmlFile = new MockMultipartFile(
                "file",
                "evil.html",
                "text/html",
                "<script>alert(1)</script>".getBytes()
        );

        // 未登录匿名上传应被拦截 (401)
        mockMvc.perform(multipart("/api/common/upload").file(htmlFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)));

        // 已登录但上传非图片扩展名应被业务拒绝 (400)
        mockMvc.perform(multipart("/api/common/upload").file(htmlFile)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("不支持的文件类型")));
    }
}
