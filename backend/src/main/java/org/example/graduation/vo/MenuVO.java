package org.example.graduation.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String path;
    private String component;
    private String icon;
    private List<MenuVO> children;
}