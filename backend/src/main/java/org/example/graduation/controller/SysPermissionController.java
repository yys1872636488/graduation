package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.mapper.SysPermissionMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionMapper sysPermissionMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<?> list() {
        return Result.success(sysPermissionMapper.selectList(null));
    }
}