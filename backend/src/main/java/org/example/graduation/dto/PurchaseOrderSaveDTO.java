package org.example.graduation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderSaveDTO {

    @NotNull(message = "请选择供应商")
    private Long supplierId;

    @NotNull(message = "请选择下单日期")
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private String remark;

    @NotEmpty(message = "请至少添加一条采购明细")
    @Valid
    private List<PurchaseOrderItemDTO> items;
}