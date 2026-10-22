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
public class MaterialIssueVO {

    private Long id;
    private String issueNo;
    private Long workOrderId;
    private String workOrderNo;
    private LocalDate issueDate;
    private String issuer;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private List<MaterialIssueItemVO> items;
}