package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDetailVO {

    private MaterialBatchVO batch;
    private List<InspectionImageVO> images;
    private List<VisionDetectionResultVO> detections;
    private List<InspectionRecordVO> records;
}