package org.example.graduation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialIssueCreateDTO {

    @NotNull(message = "请选择领料日期")
    private LocalDate issueDate;

    private String issuer;

    private String remark;

    @NotEmpty(message = "请至少添加一条领料明细")
    @Valid
    private List<MaterialIssueItemDTO> items;
}