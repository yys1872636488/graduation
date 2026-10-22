package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QualityDispositionDTO {

    @NotNull(message = "请选择成品质检记录")
    private Long inspectionId;

    @NotBlank(message = "请选择处理类型")
    private String type;

    @NotNull(message = "请输入处理数量")
    @DecimalMin(value = "0.01", message = "处理数量必须大于0")
    private BigDecimal quantity;

    private String reason;

    private String handler;

    private String remark;
}