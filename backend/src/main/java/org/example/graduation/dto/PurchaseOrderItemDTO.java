package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDTO {

    @NotNull(message = "请选择原材料")
    private Long materialId;

    @NotNull(message = "请输入采购数量")
    @DecimalMin(value = "0.01", message = "采购数量必须大于0")
    private BigDecimal quantity;

    private String unit;

    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal unitPrice;

    private String remark;
}