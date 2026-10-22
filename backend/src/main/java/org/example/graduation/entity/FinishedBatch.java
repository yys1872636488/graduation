package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("finished_batch")
public class FinishedBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;
    private Long workOrderId;
    private Long productId;
    private Long skuId;
    private BigDecimal quantity;
    private String status;
    private LocalDate productionDate;
    private Long warehouseId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}