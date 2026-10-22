package org.example.graduation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaterialInboundDTO {

    @NotNull(message = "请选择原材料批次")
    private Long batchId;

    @NotNull(message = "请选择仓库")
    private Long warehouseId;
}