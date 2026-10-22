package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal receivedQuantity;
    private BigDecimal remainingQuantity;
    private String remark;
}