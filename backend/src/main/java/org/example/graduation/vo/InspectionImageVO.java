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
public class InspectionImageVO {

    private Long id;
    private Long batchId;
    private String originalName;
    private String url;
    private String contentType;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime createdAt;
}