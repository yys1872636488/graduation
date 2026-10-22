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
public class FinishedBatchMaterialVO {

    private Long materialBatchId;
    private String batchNo;
    private String materialCode;
    private String materialName;
    private BigDecimal quantity;
    private String unit;
}