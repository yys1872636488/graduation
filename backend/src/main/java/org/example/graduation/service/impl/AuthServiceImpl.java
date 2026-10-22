package org.example.graduation.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.dto.LoginDTO;
import org.example.graduation.entity.SysPermission;
import org.example.graduation.entity.SysUser;
import org.example.graduation.mapper.SysPermissionMapper;
import org.example.graduation.mapper.SysUserMapper;
import org.example.graduation.security.JwtTokenProvider;
import org.example.graduation.security.LoginAttemptService;
import org.example.graduation.service.AuthService;
import org.example.graduation.vo.LoginVO;
import org.example.graduation.vo.MenuVO;
import org.example.graduation.vo.UserInfoVO;
import org.example.graduation.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Override
    public LoginVO login(LoginDTO loginDTO, String clientIp) {
        String attemptKey = loginDTO.getUsername() + "|" + (clientIp == null ? "" : clientIp);
        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new BusinessException(429, "登录失败次数过多，请15分钟后再试");
        }

        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername())
                        .eq(SysUser::getStatus, 1)
        );
        if (user == null) {
            loginAttemptService.loginFailed(attemptKey);
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(attemptKey);
            throw new BusinessException("用户名或密码错误");
        }

        loginAttemptService.loginSucceeded(attemptKey);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new LoginVO(token);
    }

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(userId);

        // 构建用户信息
        UserVO userVO = UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();

        // 构建菜单树
        List<SysPermission> menus = sysPermissionMapper.selectMenusByUserId(userId);
        List<MenuVO> menuTree = buildMenuTree(menus);

        return UserInfoVO.builder()
                .user(userVO)
                .roles(roles)
                .permissions(permissions)
                .menus(menuTree)
                .build();
    }

    @Override
    public void logout(Long userId) {
        // 无状态 JWT，客户端删除 token 即可
    }

    private List<MenuVO> buildMenuTree(List<SysPermission> menus) {
        Map<Long, List<SysPermission>> grouped = menus.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        List<MenuVO> roots = grouped.getOrDefault(0L, new ArrayList<>()).stream()
                .sorted(Comparator.comparingInt(SysPermission::getSortOrder))
                .map(p -> convertToMenuVO(p, grouped))
                .collect(Collectors.toList());

        return roots;
    }

    private MenuVO convertToMenuVO(SysPermission permission, Map<Long, List<SysPermission>> grouped) {
        List<MenuVO> children = grouped.getOrDefault(permission.getId(), new ArrayList<>()).stream()
                .sorted(Comparator.comparingInt(SysPermission::getSortOrder))
                .map(child -> convertToMenuVO(child, grouped))
                .collect(Collectors.toList());

        return MenuVO.builder()
                .id(permission.getId())
                .parentId(permission.getParentId())
                .code(permission.getPermCode())
                .name(permission.getPermName())
                .path(permission.getPath())
                .component(permission.getComponent())
                .icon(permission.getIcon())
                .children(children.isEmpty() ? null : children)
                .build();
    }
}