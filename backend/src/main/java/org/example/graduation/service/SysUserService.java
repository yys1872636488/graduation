package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.UserCreateDTO;
import org.example.graduation.dto.UserUpdateDTO;
import org.example.graduation.vo.UserVO;

import java.util.List;

public interface SysUserService {

    Page<UserVO> listUsers(Integer page, Integer size, String keyword, Integer status);

    UserVO createUser(UserCreateDTO dto);

    UserVO updateUser(Long id, UserUpdateDTO dto);

    void disableUser(Long id);

    void enableUser(Long id);

    void resetPassword(Long id, String newPassword);

    List<Long> getRoleIds(Long userId);
}