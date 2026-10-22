package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_image")
public class InspectionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    private String originalName;
    private String storedPath;
    private String contentType;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    private Long createdBy;
    private LocalDateTime createdAt;
}