package org.example.graduation.service;

import org.example.graduation.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    List<SysRole> listAll();

    SysRole getById(Long id);

    List<Long> getPermissionIdsByRoleId(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}