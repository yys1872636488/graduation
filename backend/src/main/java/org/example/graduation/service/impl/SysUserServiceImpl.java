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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
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
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setRealName(dto.getRealName() != null ? dto.getRealName() : user.getRealName());
        user.setPhone(dto.getPhone() != null ? dto.getPhone() : user.getPhone());
        user.setEmail(dto.getEmail() != null ? dto.getEmail() : user.getEmail());
        sysUserMapper.updateById(user);

        // 更新角色
        if (dto.getRoleIds() != null) {
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
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }

    @Override
    public void enableUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(1);
        sysUserMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
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
}