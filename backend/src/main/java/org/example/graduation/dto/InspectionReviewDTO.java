package org.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionReviewDTO {

    private Long imageId;

    private Long detectionId;

    @NotBlank(message = "请选择质检结论")
    private String conclusion;

    @NotBlank(message = "请填写复核意见")
    @Size(max = 1000, message = "复核意见不能超过1000个字符")
    private String opinion;

    @Size(max = 50, message = "处理方式不能超过50个字符")
    private String disposition;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}