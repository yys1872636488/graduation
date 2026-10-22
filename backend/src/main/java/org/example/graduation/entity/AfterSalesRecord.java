package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_record")
public class AfterSalesRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;
    private Long salesOrderId;
    private Long shipmentId;
    private Long finishedBatchId;
    private String type;
    private String description;
    private LocalDate applyDate;
    private String status;
    private String conclusion;
    private String acceptanceResult;
    private String handler;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}