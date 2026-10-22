package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.SysPermission;
import org.example.graduation.entity.SysRole;
import org.example.graduation.mapper.SysPermissionMapper;
import org.example.graduation.mapper.SysRoleMapper;
import org.example.graduation.service.SysRoleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SysRole> listAll() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Override
    public SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        List<SysPermission> perms = sysPermissionMapper.selectByRoleId(roleId);
        return perms.stream().map(SysPermission::getId).toList();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 删除旧权限
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        // 添加新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                        roleId, permId);
            }
        }
    }
}