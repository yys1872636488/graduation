package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public Result<?> list() {
        return Result.success(sysRoleService.listAll());
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<?> getPermissions(@PathVariable Long id) {
        return Result.success(sysRoleService.getPermissionIdsByRoleId(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<?> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        sysRoleService.assignPermissions(id, body.get("permissionIds"));
        return Result.success();
    }
}