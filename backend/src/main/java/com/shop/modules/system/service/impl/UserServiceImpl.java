package com.shop.modules.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.ResultCode;
import com.shop.modules.auth.dto.UserProfileVO;
import com.shop.modules.system.entity.SysUser;
import com.shop.modules.system.mapper.SysUserMapper;
import com.shop.modules.system.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    public UserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public PageResult<UserProfileVO> pageUsers(int page, int size, String keyword, String role, Integer status) {
        int safePage = Math.max(page, 1);
        int safeSize = (size < 1) ? 10 : Math.min(size, 100);
        Page<SysUser> pageParam = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword.trim())
                    .or()
                    .like(SysUser::getNickname, keyword.trim()));
        }

        if (StringUtils.hasText(role)) {
            queryWrapper.eq(SysUser::getRole, role.trim());
        }

        if (status != null) {
            queryWrapper.eq(SysUser::getStatus, status);
        }

        queryWrapper.orderByDesc(SysUser::getId);

        Page<SysUser> resultPage = sysUserMapper.selectPage(pageParam, queryWrapper);

        List<UserProfileVO> voList = resultPage.getRecords().stream()
                .map(UserProfileVO::from)
                .toList();

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (userId == null || status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法请求参数");
        }

        // 禁止管理员禁用当前操作人自己的账号
        if (StpUtil.isLogin() && Long.valueOf(StpUtil.getLoginIdAsLong()).equals(userId) && status == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不可禁用当前登录的管理员自身账号");
        }

        SysUser targetUser = sysUserMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "指定用户不存在");
        }

        targetUser.setStatus(status);
        sysUserMapper.updateById(targetUser);

        // 如果禁用账号，立即强制踢下线已存在的登录会话
        if (status == 0) {
            try {
                StpUtil.logout(userId);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public UserProfileVO getUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return UserProfileVO.from(user);
    }
}
