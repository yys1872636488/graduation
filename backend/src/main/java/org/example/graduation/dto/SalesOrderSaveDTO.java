package org.example.graduation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderSaveDTO {

    @NotBlank(message = "请输入客户名称")
    private String customerName;

    @NotNull(message = "请选择下单日期")
    private LocalDate orderDate;

    private String remark;

    @NotEmpty(message = "请至少添加一条订单明细")
    @Valid
    private List<SalesOrderItemDTO> items;
}