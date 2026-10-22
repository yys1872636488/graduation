package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WorkOrderSaveDTO {

    private Long planId;

    @NotNull(message = "请选择产品")
    private Long productId;

    @NotNull(message = "请选择SKU")
    private Long skuId;

    @NotNull(message = "请输入计划生产数量")
    @DecimalMin(value = "0.01", message = "计划生产数量必须大于0")
    private BigDecimal plannedQuantity;

    private String manager;

    private LocalDate startDate;

    private LocalDate expectedEndDate;

    private String remark;
}