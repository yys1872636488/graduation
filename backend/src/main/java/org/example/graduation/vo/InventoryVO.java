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
public class InventoryVO {

    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private String itemType;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long skuId;
    private String skuCode;
    private String productName;
    private Long batchId;
    private String batchNo;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private String unit;
    private LocalDateTime updatedAt;
}