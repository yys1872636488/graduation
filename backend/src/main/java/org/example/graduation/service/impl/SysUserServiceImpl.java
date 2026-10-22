package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.dto.UserCreateDTO;
import org.example.graduation.dto.UserUpdateDTO;
import org.example.graduation.entity.SysRole;
import org.example.graduation.entity.SysUser;
import org.example.graduation.mapper.SysRoleMapper;
import org.example.graduation.mapper.SysUserMapper;
import org.example.graduation.service.SysUserService;
import org.example.graduation.vo.UserVO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> listUsers(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), q -> q
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword))
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> userPage = sysUserMapper.selectPage(new Page<>(page, size), wrapper);

        List<UserVO> records = userPage.getRecords().stream()
                .map(user -> {
                    List<String> roleNames = sysUserMapper.selectRoleCodesByUserId(user.getId()).stream()
                            .map(code -> {
                                SysRole role = sysRoleMapper.selectOne(
                                        new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code));
                                return role != null ? role.getRoleName() : code;
                            })
                            .toList();
                    return UserVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .realName(user.getRealName())
                            .phone(user.getPhone())
                            .email(user.getEmail())
                            .status(user.getStatus())
                            .createdAt(user.getCreatedAt())
                            .roleNames(roleNames)
                            .build();
                })
                .toList();

        Page<UserVO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional
    public UserVO createUser(UserCreateDTO dto) {
        // 检查用户名唯一
        if (sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName() != null ? dto.getRealName() : "");
        user.setPhone(dto.getPhone() != null ? dto.getPhone() : "");
        user.setEmail(dto.getEmail() != null ? dto.getEmail() : "");
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 分配角色（非管理员不能分配超出自身权限的角色）
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assertRolesAssignable(dto.getRoleIds());
            for (Long roleId : dto.getRoleIds()) {
                sysUserMapper.insertUserRole(user.getId(), roleId);
            }
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .status(user.getStatus())
                .build();
    }

    @Override
    @Transactional
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        Long callerId = currentUserId();
        boolean editingSelf = callerId.equals(id);
        // 非本人必须从入口处校验管理权限，不能等到角色变更时才校验
        if (!editingSelf) {
            assertCanManageTarget(id);
        }

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setRealName(dto.getRealName() != null ? dto.getRealName() : user.getRealName());
        user.setPhone(dto.getPhone() != null ? dto.getPhone() : user.getPhone());
        user.setEmail(dto.getEmail() != null ? dto.getEmail() : user.getEmail());
        sysUserMapper.updateById(user);

        // 更新角色：不允许修改自己的角色，且不能分配超出自身权限的角色
        if (dto.getRoleIds() != null) {
            if (editingSelf) {
                throw new AccessDeniedException("不能修改自己的角色");
            }
            assertRolesAssignable(dto.getRoleIds());
            sysUserMapper.deleteUserRolesByUserId(id);
            for (Long roleId : dto.getRoleIds()) {
                sysUserMapper.insertUserRole(id, roleId);
            }
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .status(user.getStatus())
                .build();
    }

    @Override
    public void disableUser(Long id) {
        assertCanManageTarget(id);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }

    @Override
    public void enableUser(Long id) {
        assertCanManageTarget(id);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(1);
        sysUserMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        assertCanManageTarget(id);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        return sysUserMapper.selectRoleIdsByUserId(userId);
    }

    /**
     * 校验当前登录用户是否有权管理目标账号：
     * - 管理员可管理任意账号，但不能通过用户管理接口操作自己；
     * - 非管理员只能管理角色权限严格低于自己的账号，且不能管理管理员账号。
     */
    private void assertCanManageTarget(Long targetId) {
        Long callerId = currentUserId();
        if (callerId.equals(targetId)) {
            throw new AccessDeniedException("不能通过用户管理接口操作自己的账号");
        }
        if (isAdmin(callerId)) {
            return;
        }
        List<String> callerRoles = sysUserMapper.selectRoleCodesByUserId(callerId);
        List<String> targetRoles = sysUserMapper.selectRoleCodesByUserId(targetId);
        if (targetRoles.contains("ADMIN")) {
            throw new AccessDeniedException("无权操作管理员账号");
        }
        if (!callerRoles.containsAll(targetRoles)
                || new HashSet<>(callerRoles).equals(new HashSet<>(targetRoles))) {
            throw new AccessDeniedException("无权操作同级或更高权限的账号");
        }
    }

    /**
     * 非管理员只能分配自己已经拥有的角色，防止通过创建/编辑用户提权。
     */
    private void assertRolesAssignable(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long callerId = currentUserId();
        if (isAdmin(callerId)) {
            return;
        }
        List<Long> callerRoleIds = sysUserMapper.selectRoleIdsByUserId(callerId);
        if (!callerRoleIds.containsAll(roleIds)) {
            throw new AccessDeniedException("不能分配超出自身权限的角色");
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("未认证");
        }
        SysUser caller = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, authentication.getName()));
        if (caller == null) {
            throw new AccessDeniedException("当前用户不存在");
        }
        return caller.getId();
    }

    private boolean isAdmin(Long userId) {
        return sysUserMapper.selectRoleCodesByUserId(userId).contains("ADMIN");
    }
}