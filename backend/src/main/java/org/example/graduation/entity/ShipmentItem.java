package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipment_item")
public class ShipmentItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shipmentId;
    private Long finishedBatchId;
    private Long skuId;
    private BigDecimal quantity;
    private String remark;
    private LocalDateTime createdAt;
}