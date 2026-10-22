package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShipmentItemDTO {

    @NotNull(message = "请选择成品批次")
    private Long finishedBatchId;

    @NotNull(message = "请输入发货数量")
    @DecimalMin(value = "0.01", message = "发货数量必须大于0")
    private BigDecimal quantity;

    private String remark;
}