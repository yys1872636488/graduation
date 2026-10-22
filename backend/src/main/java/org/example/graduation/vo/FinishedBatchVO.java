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
public class FinishedBatchVO {

    private Long id;
    private String batchNo;
    private Long workOrderId;
    private String workOrderNo;
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuCode;
    private BigDecimal quantity;
    private String status;
    private LocalDate productionDate;
    private Long warehouseId;
    private String remark;
    private LocalDateTime createdAt;
    private List<FinishedBatchMaterialVO> materials;
}