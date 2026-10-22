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
public class ProductionProgressVO {

    private Long id;
    private Long workOrderId;
    private String workOrderNo;
    private LocalDate progressDate;
    private String processName;
    private BigDecimal completedQuantity;
    private BigDecimal progressPercent;
    private String recorder;
    private String remark;
    private LocalDateTime createdAt;
}