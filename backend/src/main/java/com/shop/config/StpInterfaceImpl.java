package com.shop.config;

import cn.dev33.satoken.stp.StpInterface;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 自定义权限与角色加载接口实现
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public StpInterfaceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        try {
            Long userId = Long.valueOf(loginId.toString());
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && user.getRole() != null && user.getStatus() == 1) {
                return List.of(user.getRole());
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }
}
