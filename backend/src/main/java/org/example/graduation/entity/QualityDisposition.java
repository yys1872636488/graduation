package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("quality_disposition")
public class QualityDisposition {

    @TableId(type = IdType.AUTO)
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