package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderVO {

    private Long id;
    private String workOrderNo;
    private Long planId;
    private String planNo;
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuCode;
    private BigDecimal plannedQuantity;
    private BigDecimal completedQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal inspectedQuantity;
    private BigDecimal uninspectedQuantity;
    private BigDecimal batchedQuantity;
    private BigDecimal pendingBatchQuantity;
    private String currentProcess;
    private String manager;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private String status;
    private String remark;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String cancelReason;
    private LocalDateTime createdAt;
    private Integer issueCount;
    private Integer inspectionCount;
    private Integer batchCount;
    private List<MaterialIssueVO> issues;
    private List<ProductionProgressVO> progressRecords;
    private List<FinishedInspectionVO> inspections;
    private List<FinishedBatchVO> batches;
}