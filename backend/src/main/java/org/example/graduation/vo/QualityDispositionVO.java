package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityDispositionVO {

    private Long id;
    private Long inspectionId;
    private Long workOrderId;
    private String type;
    private BigDecimal quantity;
    private String reason;
    private String handler;
    private LocalDateTime handledAt;
    private String remark;
    private LocalDateTime createdAt;
}