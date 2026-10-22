package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionPlanReleaseDTO {

    /** 本次生成工单的数量；为空则使用计划剩余数量 */
    @DecimalMin(value = "0.01", message = "工单数量必须大于0")
    private BigDecimal quantity;
}