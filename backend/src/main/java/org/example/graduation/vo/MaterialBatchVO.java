package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialBatchVO {

    private Long id;
    private String batchNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private Long supplierId;
    private String supplierName;
    private Long purchaseOrderId;
    private String orderNo;
    private Long arrivalId;
    private String arrivalNo;
    private String supplierBatchNo;
    private BigDecimal quantity;
    private String unit;
    private LocalDate arrivalDate;
    private String inspectionStatus;
    private BigDecimal availableQuantity;
    private LocalDateTime createdAt;
}