package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("shipment")
public class Shipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shipmentNo;
    private Long orderId;
    private LocalDate shipmentDate;
    private String logisticsNo;
    private String operator;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
}