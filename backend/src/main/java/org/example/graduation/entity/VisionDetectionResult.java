package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vision_detection_result")
public class VisionDetectionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    private Long imageId;
    private String modelName;
    private String modelVersion;
    private String status;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer detectionCount;
    private String detectionsJson;
    private String annotatedImagePath;
    private Integer inferenceTimeMs;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
}