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
public class InventoryTransactionVO {

    private Long id;
    private String transactionNo;
    private Long warehouseId;
    private String warehouseName;
    private String itemType;
    private Long materialId;
    private String materialName;
    private Long skuId;
    private String skuCode;
    private Long batchId;
    private String batchNo;
    private String businessType;
    private String direction;
    private BigDecimal quantity;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String businessNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}