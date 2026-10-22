package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishedInspectionVO {

    private Long id;
    private String inspectionNo;
    private Long workOrderId;
    private String workOrderNo;
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuCode;
    private BigDecimal inspectedQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal unqualifiedQuantity;
    private String result;
    private String inspector;
    private LocalDateTime inspectionTime;
    private String remark;
    private LocalDateTime createdAt;
}