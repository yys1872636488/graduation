package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FinishedInspectionDTO {

    @NotNull(message = "请输入送检数量")
    @DecimalMin(value = "0.01", message = "送检数量必须大于0")
    private BigDecimal inspectedQuantity;

    @NotNull(message = "请输入合格数量")
    @DecimalMin(value = "0", message = "合格数量不能为负数")
    private BigDecimal qualifiedQuantity;

    @NotNull(message = "请输入不合格数量")
    @DecimalMin(value = "0", message = "不合格数量不能为负数")
    private BigDecimal unqualifiedQuantity;

    private String inspector;

    private LocalDateTime inspectionTime;

    private String remark;
}