package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warehouseId;
    private String itemType;
    private Long materialId;
    private Long skuId;
    private Long batchId;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}