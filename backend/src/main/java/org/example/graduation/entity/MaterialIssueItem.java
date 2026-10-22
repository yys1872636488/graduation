package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_issue_item")
public class MaterialIssueItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long issueId;
    private Long materialBatchId;
    private Long materialId;
    private BigDecimal quantity;
    private String unit;
    private String remark;
    private LocalDateTime createdAt;
}