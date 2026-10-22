package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSalesVO {

    private Long id;
    private String recordNo;
    private Long salesOrderId;
    private String orderNo;
    private Long shipmentId;
    private String shipmentNo;
    private Long finishedBatchId;
    private String finishedBatchNo;
    private String type;
    private String description;
    private LocalDate applyDate;
    private String status;
    private String conclusion;
    private String acceptanceResult;
    private String handler;
    private String remark;
    private LocalDateTime createdAt;
}