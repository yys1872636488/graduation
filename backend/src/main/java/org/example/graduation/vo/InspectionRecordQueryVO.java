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
public class InspectionRecordQueryVO {

    private Long id;
    private Long batchId;
    private String batchNo;
    private String materialCode;
    private String materialName;
    private String supplierName;
    private String conclusion;
    private String opinion;
    private String disposition;
    private String remark;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private Long imageId;
    private Long detectionId;
}