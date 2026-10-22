package org.example.graduation.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserInfoVO {
    private UserVO user;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuVO> menus;
}