package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("material_batch")
public class MaterialBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;
    private Long materialId;
    private Long supplierId;
    private Long purchaseOrderId;
    private Long purchaseOrderItemId;
    private Long arrivalId;
    private String supplierBatchNo;
    private BigDecimal quantity;
    private String unit;
    private LocalDate arrivalDate;
    private String inspectionStatus;
    private BigDecimal availableQuantity;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}