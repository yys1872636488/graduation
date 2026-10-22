package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("production_progress")
public class ProductionProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;
    private LocalDate progressDate;
    private String processName;
    private BigDecimal completedQuantity;
    private BigDecimal progressPercent;
    private String recorder;
    private String remark;
    private LocalDateTime createdAt;
}