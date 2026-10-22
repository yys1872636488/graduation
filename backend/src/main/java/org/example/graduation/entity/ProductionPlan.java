package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("production_plan")
public class ProductionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;
    private Long productId;
    private Long skuId;
    private BigDecimal plannedQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String priority;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime releasedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}