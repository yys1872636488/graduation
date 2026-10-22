package org.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AfterSalesProcessDTO {

    @NotBlank(message = "请选择处理状态")
    private String status;

    private String conclusion;

    private String acceptanceResult;

    private String handler;

    private String remark;
}