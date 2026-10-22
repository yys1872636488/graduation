package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("finished_batch_material")
public class FinishedBatchMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long finishedBatchId;
    private Long materialBatchId;
    private BigDecimal quantity;
    private LocalDateTime createdAt;
}