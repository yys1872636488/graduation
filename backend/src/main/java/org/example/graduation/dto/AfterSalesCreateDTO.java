package org.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AfterSalesCreateDTO {

    @NotNull(message = "请选择销售订单")
    private Long salesOrderId;

    private Long shipmentId;

    private Long finishedBatchId;

    @NotBlank(message = "请选择售后类型")
    private String type;

    private String description;

    @NotNull(message = "请选择申请日期")
    private LocalDate applyDate;

    private String handler;

    private String remark;
}