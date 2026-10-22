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
public class SalesOrderVO {

    private Long id;
    private String orderNo;
    private String customerName;
    private LocalDate orderDate;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private List<SalesOrderItemVO> items;
}