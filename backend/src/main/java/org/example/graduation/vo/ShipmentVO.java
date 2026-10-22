package org.example.graduation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentVO {

    private Long id;
    private String shipmentNo;
    private Long orderId;
    private String orderNo;
    private String customerName;
    private LocalDate shipmentDate;
    private String logisticsNo;
    private String operator;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private List<ShipmentItemVO> items;
}