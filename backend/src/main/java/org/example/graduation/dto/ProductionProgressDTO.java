package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductionProgressDTO {

    @NotNull(message = "请选择进度日期")
    private LocalDate progressDate;

    private String processName;

    @NotNull(message = "请输入累计完成数量")
    @DecimalMin(value = "0", message = "完成数量不能为负数")
    private BigDecimal completedQuantity;

    private BigDecimal progressPercent;

    private String recorder;

    private String remark;
}