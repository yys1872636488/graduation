package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_transaction")
public class InventoryTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transactionNo;
    private Long warehouseId;
    private String itemType;
    private Long materialId;
    private Long skuId;
    private Long batchId;
    private String businessType;
    private String direction;
    private BigDecimal quantity;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private String businessNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}