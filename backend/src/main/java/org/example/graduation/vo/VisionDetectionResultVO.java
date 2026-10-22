package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionDetectionResultVO {

    private Long id;
    private Long batchId;
    private Long imageId;
    private String modelName;
    private String modelVersion;
    private String status;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer detectionCount;
    private List<Map<String, Object>> detections;
    private String annotatedImagePath;
    private Integer inferenceTimeMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}