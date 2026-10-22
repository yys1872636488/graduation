package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("arrival_record")
public class ArrivalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String arrivalNo;
    private Long orderId;
    private Long orderItemId;
    private Long materialId;
    private Long supplierId;
    private BigDecimal quantity;
    private String unit;
    private LocalDate arrivalDate;
    private String supplierBatchNo;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
}