package org.example.graduation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderCancelDTO {

    @Size(max = 500, message = "取消原因不能超过500个字符")
    private String reason;
}