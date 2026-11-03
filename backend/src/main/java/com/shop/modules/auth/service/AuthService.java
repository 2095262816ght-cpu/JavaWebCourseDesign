package com.shop.modules.auth.service;

import com.shop.modules.auth.dto.AuthTokenVO;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import com.shop.modules.auth.dto.UpdateProfileCommand;
import com.shop.modules.auth.dto.UserProfileVO;

/**
 * 统一认证与会话服务（高内聚深模块接口）
 */
public interface AuthService {

    /**
     * 顾客新账号注册
     */
    UserProfileVO register(RegisterCommand cmd);

    /**
     * 用户登录认证（支持管理员与顾客）
     */
    AuthTokenVO login(LoginCommand cmd);

    /**
     * 获取当前登录用户资料
     */
    UserProfileVO getCurrentUser();

    /**
     * 修改当前登录用户资料或密码
     */
    UserProfileVO updateCurrentUserProfile(UpdateProfileCommand cmd);

    /**
     * 注销当前用户登录会话
     */
    void logout();
}
