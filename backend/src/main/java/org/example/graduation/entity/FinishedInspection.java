package org.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("finished_inspection")
public class FinishedInspection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String inspectionNo;
    private Long workOrderId;
    private Long productId;
    private Long skuId;
    private BigDecimal inspectedQuantity;
    private BigDecimal qualifiedQuantity;
    private BigDecimal unqualifiedQuantity;
    private String result;
    private String inspector;
    private LocalDateTime inspectionTime;
    private String remark;
    private LocalDateTime createdAt;
}