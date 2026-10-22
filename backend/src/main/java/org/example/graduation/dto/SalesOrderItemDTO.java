package org.example.graduation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItemDTO {

    @NotNull(message = "请选择SKU")
    private Long skuId;

    @NotNull(message = "请输入订单数量")
    @DecimalMin(value = "0.01", message = "订单数量必须大于0")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal unitPrice;

    private String remark;
}