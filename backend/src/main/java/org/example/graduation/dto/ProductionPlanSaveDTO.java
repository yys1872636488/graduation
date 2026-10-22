package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductionPlanSaveDTO {

    @NotNull(message = "请选择产品")
    private Long productId;

    @NotNull(message = "请选择SKU")
    private Long skuId;

    @NotNull(message = "请输入计划数量")
    @DecimalMin(value = "0.01", message = "计划数量必须大于0")
    private BigDecimal plannedQuantity;

    @NotNull(message = "请选择计划开始日期")
    private LocalDate startDate;

    private LocalDate endDate;

    private String priority;

    private String remark;
}