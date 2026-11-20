package com.shop.modules.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import com.shop.modules.auth.dto.AuthTokenVO;
import com.shop.modules.auth.dto.LoginCommand;
import com.shop.modules.auth.dto.RegisterCommand;
import com.shop.modules.auth.dto.UpdateProfileCommand;
import com.shop.modules.auth.dto.UserProfileVO;
import com.shop.modules.auth.service.AuthService;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_CUSTOMER_AVATAR = "/uploads/avatar-default.jpg";

    private final SysUserMapper sysUserMapper;

    public AuthServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO register(RegisterCommand cmd) {
        String username = cmd.username() != null ? cmd.username().trim() : "";
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }

        // 1. 唯一性校验：确保用户名未被占用
        Long existingCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名 [" + username + "] 已被注册，请更换用户名");
        }

        // 2. 密码安全加盐散列（BCrypt 算法）
        String saltHashedPassword = BCrypt.hashpw(cmd.password(), BCrypt.gensalt());

        // 3. 构建用户实体并持久化
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(saltHashedPassword);
        user.setNickname(StringUtils.hasText(cmd.nickname()) ? cmd.nickname().trim() : username);
        user.setAvatar(StringUtils.hasText(cmd.avatar()) ? cmd.avatar().trim() : DEFAULT_CUSTOMER_AVATAR);
        user.setRole("ROLE_CUSTOMER");
        user.setStatus(1); // 默认启用

        sysUserMapper.insert(user);
        return UserProfileVO.from(user);
    }

    @Override
    public AuthTokenVO login(LoginCommand cmd) {
        String username = cmd.username() != null ? cmd.username().trim() : "";
        // 1. 检索用户记录
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        // 2. 验证用户存在及密码匹配性（防定时攻击与明文泄露）
        if (user == null || !BCrypt.checkpw(cmd.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        // 3. 状态检查（是否被管理员封禁）
        if (!user.isActive()) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "该账号已被禁用，请联系平台管理员");
        }

        // 4. 执行 Sa-Token 登录认证
        StpUtil.login(user.getId());
        StpUtil.getSession().set("user", user);

        String tokenName = StpUtil.getTokenName();
        String tokenValue = StpUtil.getTokenValue();

        return new AuthTokenVO(tokenName, tokenValue, UserProfileVO.from(user));
    }

    @Override
    public UserProfileVO getCurrentUser() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户资料不存在或登录已过期");
        }

        if (!user.isActive()) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用，已被强制下线");
        }

        return UserProfileVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentUserProfile(UpdateProfileCommand cmd) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在");
        }
        if (!user.isActive()) {
            StpUtil.logout();
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用，无法修改个人资料");
        }

        // 密码修改逻辑（若传递了新密码）
        if (StringUtils.hasText(cmd.newPassword())) {
            String trimmedNewPassword = cmd.newPassword().trim();
            if (trimmedNewPassword.length() < 6 || trimmedNewPassword.length() > 32) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "新密码长度须在6至32位之间");
            }
            if (!StringUtils.hasText(cmd.oldPassword())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "修改密码必须输入原密码");
            }
            if (!BCrypt.checkpw(cmd.oldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "原密码验证不通过");
            }
            user.setPassword(BCrypt.hashpw(trimmedNewPassword, BCrypt.gensalt()));
        }

        if (StringUtils.hasText(cmd.nickname())) {
            user.setNickname(cmd.nickname().trim());
        }

        if (StringUtils.hasText(cmd.avatar())) {
            user.setAvatar(cmd.avatar().trim());
        }

        sysUserMapper.updateById(user);
        StpUtil.getSession().set("user", user);

        return UserProfileVO.from(user);
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }
}
