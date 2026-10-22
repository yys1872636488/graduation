package org.example.graduation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InspectionDetectDTO {

    @NotNull(message = "请选择要检测的图片")
    private Long imageId;
}