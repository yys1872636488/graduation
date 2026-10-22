package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.UserCreateDTO;
import org.example.graduation.dto.UserUpdateDTO;
import org.example.graduation.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.listUsers(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public Result<?> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(sysUserService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<?> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        return Result.success(sysUserService.updateUser(id, dto));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('user:disable')")
    public Result<?> disable(@PathVariable Long id) {
        sysUserService.disableUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('user:disable')")
    public Result<?> enable(@PathVariable Long id) {
        sysUserService.enableUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:reset-password')")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }
        sysUserService.resetPassword(id, newPassword);
        return Result.success();
    }

    @GetMapping("/{id}/role-ids")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<?> getRoleIds(@PathVariable Long id) {
        return Result.success(sysUserService.getRoleIds(id));
    }
}