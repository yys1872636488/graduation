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
public class ProductionPlanVO {

    private Long id;
    private String planNo;
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuCode;
    private BigDecimal plannedQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String priority;
    private String status;
    private String remark;
    private Integer workOrderCount;
    private BigDecimal workOrderQuantity;
    private BigDecimal remainingQuantity;
    private LocalDateTime releasedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}