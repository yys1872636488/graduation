package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRecordVO {

    private Long id;
    private Long batchId;
    private Long imageId;
    private Long detectionId;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String conclusion;
    private String opinion;
    private String disposition;
    private String remark;
    private LocalDateTime createdAt;
}