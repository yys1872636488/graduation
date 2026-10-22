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
public class ArrivalVO {

    private Long id;
    private String arrivalNo;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String supplierName;
    private BigDecimal quantity;
    private String unit;
    private LocalDate arrivalDate;
    private String supplierBatchNo;
    private Long batchId;
    private String batchNo;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
}