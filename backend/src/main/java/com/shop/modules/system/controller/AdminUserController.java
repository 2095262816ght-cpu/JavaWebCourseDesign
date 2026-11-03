package com.shop.modules.system.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.modules.auth.dto.UserProfileVO;
import com.shop.modules.system.dto.UserStatusUpdateDTO;
import com.shop.modules.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-用户管理", description = "系统管理员对平台顾客与管理员账号的查询与启停控制")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户列表", description = "仅管理员可用：支持用户名/昵称模糊搜索、按角色与状态筛选")
    @GetMapping
    public Result<PageResult<UserProfileVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status
    ) {
        PageResult<UserProfileVO> pageResult = userService.pageUsers(page, size, keyword, role, status);
        return Result.success(pageResult);
    }

    @Operation(summary = "修改用户启用/禁用状态", description = "仅管理员可用：禁用后用户将立即被强制注销且无法再登录")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO dto
    ) {
        userService.updateUserStatus(id, dto.status());
        return Result.success("用户状态修改成功", null);
    }

    @Operation(summary = "查询用户详情", description = "仅管理员可用：查询指定用户的详细资料")
    @GetMapping("/{id}")
    public Result<UserProfileVO> getUserDetail(@PathVariable Long id) {
        UserProfileVO vo = userService.getUserById(id);
        return Result.success(vo);
    }
}
