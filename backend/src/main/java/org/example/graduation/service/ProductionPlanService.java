package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.ProductionPlanSaveDTO;
import org.example.graduation.vo.ProductionPlanVO;
import org.example.graduation.vo.ProductionStatusLogVO;
import org.example.graduation.vo.WorkOrderVO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductionPlanService {

    Page<ProductionPlanVO> list(Integer page, Integer size, String keyword, String status);

    ProductionPlanVO create(ProductionPlanSaveDTO dto);

    ProductionPlanVO update(Long id, ProductionPlanSaveDTO dto);

    /** 下达或追加工单；quantity 为空时使用计划剩余数量 */
    WorkOrderVO release(Long id, BigDecimal quantity);

    void cancel(Long id);

    List<ProductionStatusLogVO> statusLogs(Long id);
}