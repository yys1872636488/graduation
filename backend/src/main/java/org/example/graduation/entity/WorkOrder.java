package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workOrderNo;
    private Long planId;
    private Long productId;
    private Long skuId;
    private BigDecimal plannedQuantity;
    private BigDecimal completedQuantity;
    private BigDecimal qualifiedQuantity;
    private String manager;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}