package org.example.graduation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ShipmentCreateDTO {

    @NotNull(message = "请选择发货日期")
    private LocalDate shipmentDate;

    private String logisticsNo;

    private String operator;

    private String remark;

    @NotEmpty(message = "请至少添加一条发货明细")
    @Valid
    private List<ShipmentItemDTO> items;
}