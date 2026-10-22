package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("material_issue")
public class MaterialIssue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String issueNo;
    private Long workOrderId;
    private LocalDate issueDate;
    private String issuer;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
}