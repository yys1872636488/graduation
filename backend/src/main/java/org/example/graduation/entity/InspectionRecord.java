package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    private Long imageId;
    private Long detectionId;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String conclusion;
    private String opinion;
    private String disposition;
    private String remark;
    private LocalDateTime createdAt;
}