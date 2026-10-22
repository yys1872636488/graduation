package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderVO {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private List<PurchaseOrderItemVO> items;
}