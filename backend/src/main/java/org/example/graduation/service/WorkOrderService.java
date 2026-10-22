package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.FinishedBatchCreateDTO;
import org.example.graduation.dto.FinishedInspectionDTO;
import org.example.graduation.dto.MaterialIssueCreateDTO;
import org.example.graduation.dto.ProductionProgressDTO;
import org.example.graduation.dto.QualityDispositionDTO;
import org.example.graduation.dto.WorkOrderSaveDTO;
import org.example.graduation.vo.FinishedBatchVO;
import org.example.graduation.vo.FinishedInspectionVO;
import org.example.graduation.vo.MaterialIssueVO;
import org.example.graduation.vo.ProductionProgressVO;
import org.example.graduation.vo.ProductionStatusLogVO;
import org.example.graduation.vo.QualityDispositionVO;
import org.example.graduation.vo.WorkOrderVO;

import java.util.List;

public interface WorkOrderService {

    Page<WorkOrderVO> list(Integer page, Integer size, String keyword, String status, Long planId);

    WorkOrderVO detail(Long id);

    WorkOrderVO create(WorkOrderSaveDTO dto);

    void start(Long id);

    void cancel(Long id, String reason);

    MaterialIssueVO createIssue(Long workOrderId, MaterialIssueCreateDTO dto);

    Page<MaterialIssueVO> listIssues(Integer page, Integer size, String keyword, Long workOrderId);

    ProductionProgressVO addProgress(Long workOrderId, ProductionProgressDTO dto);

    FinishedInspectionVO addInspection(Long workOrderId, FinishedInspectionDTO dto);

    Page<FinishedInspectionVO> listInspections(Integer page, Integer size, String keyword);

    QualityDispositionVO addDisposition(QualityDispositionDTO dto);

    FinishedBatchVO createBatch(Long workOrderId, FinishedBatchCreateDTO dto);

    Page<FinishedBatchVO> listBatches(Integer page, Integer size, String keyword, String status);

    List<ProductionStatusLogVO> statusLogs(Long id);
}