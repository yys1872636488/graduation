package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("perm_code")
    private String permCode;

    @TableField("perm_name")
    private String permName;

    private String type;   // menu / button
    private String path;   // 路由路径（菜单）
    private String component; // 组件路径（菜单）
    private String icon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}