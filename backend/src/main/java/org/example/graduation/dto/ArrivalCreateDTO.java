package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ArrivalCreateDTO {

    @NotNull(message = "请选择采购明细")
    private Long orderItemId;

    @NotNull(message = "请输入到货数量")
    @DecimalMin(value = "0.01", message = "到货数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "请选择到货日期")
    private LocalDate arrivalDate;

    private String supplierBatchNo;

    private String remark;
}