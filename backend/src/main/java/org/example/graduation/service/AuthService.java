package org.example.graduation.service;

import org.example.graduation.dto.ChangePasswordDTO;
import org.example.graduation.dto.LoginDTO;
import org.example.graduation.vo.LoginVO;
import org.example.graduation.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO, String clientIp);

    void changePassword(Long userId, ChangePasswordDTO dto);

    UserInfoVO getCurrentUserInfo(Long userId);

    void logout(Long userId);
}