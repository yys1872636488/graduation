package org.example.graduation.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {

    private String realName;
    private String phone;
    private String email;
    private List<Long> roleIds;
}