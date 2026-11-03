package com.shop.modules.auth;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.modules.auth.dto.AuthTokenVO;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import com.shop.modules.auth.dto.UserProfileVO;
import com.shop.modules.auth.service.AuthService;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import com.shop.modules.system.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @BeforeEach
    void setupAdminPassword() {
        // 保证 seed 的 admin 密码是真实的 BCrypt 散列串
        SysUser admin = sysUserMapper.selectById(1L);
        if (admin != null && !BCrypt.checkpw("admin123", admin.getPassword())) {
            admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
            sysUserMapper.updateById(admin);
        }

        // 保证 seed 的 customer 密码是真实的 BCrypt 散列串
        SysUser customer = sysUserMapper.selectById(2L);
        if (customer != null && !BCrypt.checkpw("123456", customer.getPassword())) {
            customer.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
            sysUserMapper.updateById(customer);
        }
    }

    @Test
    @DisplayName("测试用户注册及BCrypt加密落库")
    void testRegisterNewCustomer() {
        String testUser = "test_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterCommand cmd = new RegisterCommand(testUser, "password123", "自动化测试用户", null);

        UserProfileVO vo = authService.register(cmd);
        Assertions.assertNotNull(vo);
        Assertions.assertEquals(testUser, vo.username());
        Assertions.assertEquals("ROLE_CUSTOMER", vo.role());
        Assertions.assertEquals(1, vo.status());

        // 验证数据库中存储的是 BCrypt 密文，绝非明文
        SysUser inDb = sysUserMapper.selectById(vo.id());
        Assertions.assertNotNull(inDb);
        Assertions.assertNotEquals("password123", inDb.getPassword());
        Assertions.assertTrue(BCrypt.checkpw("password123", inDb.getPassword()));
    }

    @Test
    @DisplayName("测试重复用户名注册拦截")
    void testRegisterDuplicateUsername() {
        String duplicateUser = "dup_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterCommand cmd = new RegisterCommand(duplicateUser, "password123", "重复用户1", null);
        authService.register(cmd);

        // 第二次注册同名应抛出 BusinessException
        Assertions.assertThrows(BusinessException.class, () -> {
            authService.register(new RegisterCommand(duplicateUser, "anotherpass", "重复用户2", null));
        });
    }

    @Test
    @DisplayName("测试管理员与普通用户凭证登录及令牌签发")
    void testLoginSuccess() {
        LoginCommand adminCmd = new LoginCommand("admin", "admin123");
        AuthTokenVO tokenVO = authService.login(adminCmd);

        Assertions.assertNotNull(tokenVO);
        Assertions.assertNotNull(tokenVO.tokenValue());
        Assertions.assertEquals("admin", tokenVO.user().username());
        Assertions.assertEquals("ROLE_ADMIN", tokenVO.user().role());

        // 测试注销
        authService.logout();
        Assertions.assertFalse(StpUtil.isLogin());
    }

    @Test
    @DisplayName("测试错误密码拒绝登录")
    void testLoginWrongPassword() {
        LoginCommand wrongCmd = new LoginCommand("admin", "badpassword999");
        Assertions.assertThrows(BusinessException.class, () -> authService.login(wrongCmd));
    }

    @Test
    @DisplayName("测试管理员查询用户列表与账号启停切换")
    void testAdminUserManagement() {
        // 先以管理员身份模拟登录
        AuthTokenVO adminToken = authService.login(new LoginCommand("admin", "admin123"));
        Assertions.assertNotNull(adminToken);

        // 分页查询用户列表
        PageResult<UserProfileVO> page = userService.pageUsers(1, 10, null, null, null);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.total() >= 2);

        // 切换顾客账号(id=2)为禁用状态
        userService.updateUserStatus(2L, 0);
        UserProfileVO customerVo = userService.getUserById(2L);
        Assertions.assertEquals(0, customerVo.status());

        // 禁用状态下尝试登录应抛出被禁用异常
        Assertions.assertThrows(BusinessException.class, () -> {
            authService.login(new LoginCommand("customer", "123456"));
        });

        // 恢复启用
        userService.updateUserStatus(2L, 1);
        customerVo = userService.getUserById(2L);
        Assertions.assertEquals(1, customerVo.status());

        authService.logout();
    }
}
