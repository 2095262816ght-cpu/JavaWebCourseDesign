package com.shop.modules.system.service;

import com.shop.common.result.PageResult;
import com.shop.modules.auth.dto.UserProfileVO;

/**
 * 平台用户管理服务（管理员视角）
 */
public interface UserService {

    /**
     * 分页多条件查询平台用户列表
     */
    PageResult<UserProfileVO> pageUsers(int page, int size, String keyword, String role, Integer status);

    /**
     * 修改指定用户的账号启用/禁用状态
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 查询指定用户详情
     */
    UserProfileVO getUserById(Long userId);
}
