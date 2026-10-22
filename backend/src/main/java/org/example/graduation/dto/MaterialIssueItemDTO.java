package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialIssueItemDTO {

    @NotNull(message = "请选择原材料批次")
    private Long materialBatchId;

    @NotNull(message = "请输入领料数量")
    @DecimalMin(value = "0.01", message = "领料数量必须大于0")
    private BigDecimal quantity;

    private String remark;
}