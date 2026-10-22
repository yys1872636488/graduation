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
public class ShipmentItemVO {

    private Long id;
    private Long finishedBatchId;
    private String batchNo;
    private Long skuId;
    private String skuCode;
    private BigDecimal quantity;
    private String remark;
}