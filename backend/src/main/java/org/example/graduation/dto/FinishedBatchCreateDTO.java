package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinishedBatchCreateDTO {

    /** 本次生成数量；为空时使用全部剩余合格数量 */
    @DecimalMin(value = "0.01", message = "批次数量必须大于0")
    private BigDecimal quantity;

    private LocalDate productionDate;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}