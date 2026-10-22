package org.example.graduation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinishedInboundDTO {

    @NotNull(message = "请选择成品批次")
    private Long finishedBatchId;

    @NotNull(message = "请选择仓库")
    private Long warehouseId;
}