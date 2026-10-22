package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.service.InventoryService;
import org.example.graduation.service.ProductionWorkflowService;
import org.example.graduation.dto.FinishedBatchCreateDTO;
import org.example.graduation.dto.FinishedInspectionDTO;
import org.example.graduation.dto.MaterialIssueCreateDTO;
import org.example.graduation.dto.MaterialIssueItemDTO;
import org.example.graduation.dto.ProductionProgressDTO;
import org.example.graduation.dto.QualityDispositionDTO;
import org.example.graduation.dto.WorkOrderSaveDTO;
import org.example.graduation.entity.FinishedBatch;
import org.example.graduation.entity.FinishedBatchMaterial;
import org.example.graduation.entity.FinishedInspection;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.MaterialIssue;
import org.example.graduation.entity.MaterialIssueItem;
import org.example.graduation.entity.Product;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.entity.ProductionPlan;
import org.example.graduation.entity.ProductionProgress;
import org.example.graduation.entity.QualityDisposition;
import org.example.graduation.entity.WorkOrder;
import org.example.graduation.mapper.FinishedBatchMapper;
import org.example.graduation.mapper.FinishedBatchMaterialMapper;
import org.example.graduation.mapper.FinishedInspectionMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialIssueItemMapper;
import org.example.graduation.mapper.MaterialIssueMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.mapper.ProductSkuMapper;
import org.example.graduation.mapper.ProductionPlanMapper;
import org.example.graduation.mapper.ProductionProgressMapper;
import org.example.graduation.mapper.QualityDispositionMapper;
import org.example.graduation.mapper.WorkOrderMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.WorkOrderService;
import org.example.graduation.vo.FinishedBatchMaterialVO;
import org.example.graduation.vo.FinishedBatchVO;
import org.example.graduation.vo.FinishedInspectionVO;
import org.example.graduation.vo.MaterialIssueItemVO;
import org.example.graduation.vo.MaterialIssueVO;
import org.example.graduation.vo.ProductionProgressVO;
import org.example.graduation.vo.ProductionStatusLogVO;
import org.example.graduation.vo.QualityDispositionVO;
import org.example.graduation.vo.WorkOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Set<String> DISPOSITION_TYPES = Set.of(
            ProductionStatus.DISPOSITION_REWORK,
            ProductionStatus.DISPOSITION_RECHECK,
            ProductionStatus.DISPOSITION_SCRAP);

    private final WorkOrderMapper workOrderMapper;
    private final ProductionPlanMapper planMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final MaterialBatchMapper materialBatchMapper;
    private final MaterialMapper materialMapper;
    private final MaterialIssueMapper issueMapper;
    private final MaterialIssueItemMapper issueItemMapper;
    private final ProductionProgressMapper progressMapper;
    private final FinishedInspectionMapper inspectionMapper;
    private final QualityDispositionMapper dispositionMapper;
    private final FinishedBatchMapper batchMapper;
    private final FinishedBatchMaterialMapper batchMaterialMapper;
    private final InventoryService inventoryService;
    private final ProductionWorkflowService workflowService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<WorkOrderVO> list(Integer page, Integer size, String keyword,
                                  String status, Long planId) {
        Page<WorkOrder> orderPage = workOrderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<WorkOrder>()
                        .like(StringUtils.hasText(keyword), WorkOrder::getWorkOrderNo, keyword)
                        .eq(StringUtils.hasText(status), WorkOrder::getStatus, status)
                        .eq(planId != null, WorkOrder::getPlanId, planId)
                        .orderByDesc(WorkOrder::getCreatedAt));
        List<WorkOrder> records = orderPage.getRecords();
        Map<Long, Product> products = loadProducts(
                records.stream().map(WorkOrder::getProductId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                records.stream().map(WorkOrder::getSkuId).toList());
        Map<Long, ProductionPlan> plans = loadPlans(
                records.stream().map(WorkOrder::getPlanId).toList());
        Map<Long, BigDecimal> inspectedByOrder = loadInspectedQuantities(
                records.stream().map(WorkOrder::getId).toList());
        Map<Long, BigDecimal> batchedByOrder = loadBatchedQuantities(
                records.stream().map(WorkOrder::getId).toList());
        Map<Long, String> currentProcesses = loadCurrentProcesses(
                records.stream().map(WorkOrder::getId).toList());

        List<WorkOrderVO> vos = records.stream().map(order -> {
            WorkOrderVO vo = baseVO(order, products.get(order.getProductId()),
                    skus.get(order.getSkuId()),
                    order.getPlanId() == null ? null : plans.get(order.getPlanId()));
            BigDecimal inspected = inspectedByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            vo.setInspectedQuantity(inspected);
            vo.setUninspectedQuantity(order.getCompletedQuantity()
                    .subtract(inspected).max(BigDecimal.ZERO));
            BigDecimal batched = batchedByOrder.getOrDefault(order.getId(), BigDecimal.ZERO);
            vo.setBatchedQuantity(batched);
            vo.setPendingBatchQuantity(order.getQualifiedQuantity()
                    .subtract(batched).max(BigDecimal.ZERO));
            vo.setCurrentProcess(currentProcesses.get(order.getId()));
            vo.setIssueCount(safeInt(issueMapper.selectCount(new LambdaQueryWrapper<MaterialIssue>()
                    .eq(MaterialIssue::getWorkOrderId, order.getId()))));
            vo.setInspectionCount(safeInt(inspectionMapper.selectCount(
                    new LambdaQueryWrapper<FinishedInspection>()
                            .eq(FinishedInspection::getWorkOrderId, order.getId()))));
            vo.setBatchCount(safeInt(batchMapper.selectCount(
                    new LambdaQueryWrapper<FinishedBatch>()
                            .eq(FinishedBatch::getWorkOrderId, order.getId()))));
            return vo;
        }).toList();

        Page<WorkOrderVO> result = new Page<>(
                orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public WorkOrderVO detail(Long id) {
        WorkOrder order = requireOrder(id);
        Product product = productMapper.selectById(order.getProductId());
        ProductSku sku = skuMapper.selectById(order.getSkuId());
        ProductionPlan plan = order.getPlanId() == null
                ? null : planMapper.selectById(order.getPlanId());

        WorkOrderVO vo = baseVO(order, product, sku, plan);
        List<MaterialIssueVO> issues = loadIssuesForOrder(order.getId());
        List<ProductionProgressVO> progress = loadProgressForOrder(order.getId());
        List<FinishedInspectionVO> inspections = loadInspectionsForOrder(order.getId());
        List<FinishedBatchVO> batches = loadBatchesForOrder(order.getId());
        vo.setIssues(issues);
        vo.setProgressRecords(progress);
        vo.setInspections(inspections);
        vo.setBatches(batches);
        BigDecimal inspected = inspections.stream()
                .map(FinishedInspectionVO::getInspectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setInspectedQuantity(inspected);
        vo.setUninspectedQuantity(order.getCompletedQuantity()
                .subtract(inspected).max(BigDecimal.ZERO));
        BigDecimal batched = batches.stream()
                .map(FinishedBatchVO::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setBatchedQuantity(batched);
        vo.setPendingBatchQuantity(order.getQualifiedQuantity()
                .subtract(batched).max(BigDecimal.ZERO));
        vo.setCurrentProcess(progress.isEmpty()
                ? null : progress.get(progress.size() - 1).getProcessName());
        vo.setIssueCount(issues.size());
        vo.setInspectionCount(inspections.size());
        vo.setBatchCount(batches.size());
        return vo;
    }

    @Override
    @Transactional
    public WorkOrderVO create(WorkOrderSaveDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        ProductSku sku = skuMapper.selectById(dto.getSkuId());
        validateProductSku(product, sku, dto.getProductId(), dto.getSkuId());

        ProductionPlan plan = null;
        if (dto.getPlanId() != null) {
            plan = planMapper.selectById(dto.getPlanId());
            if (plan == null) {
                throw new BusinessException("生产计划不存在");
            }
            if (!ProductionStatus.PLAN_RELEASED.equals(plan.getStatus())
                    && !ProductionStatus.PLAN_IN_PROGRESS.equals(plan.getStatus())) {
                throw new BusinessException("只有已下达的计划可以生成工单，请先下达计划");
            }
            BigDecimal consumed = workOrderMapper.selectList(
                            new LambdaQueryWrapper<WorkOrder>()
                                    .eq(WorkOrder::getPlanId, plan.getId()))
                    .stream()
                    .filter(order -> !ProductionStatus.WO_CANCELLED.equals(order.getStatus()))
                    .map(WorkOrder::getPlannedQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (consumed.add(dto.getPlannedQuantity())
                    .compareTo(plan.getPlannedQuantity()) > 0) {
                throw new BusinessException("累计工单数量不能超过计划数量");
            }
        }
        if (dto.getExpectedEndDate() != null && dto.getStartDate() != null
                && dto.getExpectedEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("预计完成日期不能早于开始日期");
        }

        WorkOrder order = new WorkOrder();
        order.setWorkOrderNo(generateWorkOrderNo());
        order.setPlanId(dto.getPlanId());
        order.setProductId(dto.getProductId());
        order.setSkuId(dto.getSkuId());
        order.setPlannedQuantity(dto.getPlannedQuantity());
        order.setCompletedQuantity(BigDecimal.ZERO);
        order.setQualifiedQuantity(BigDecimal.ZERO);
        order.setManager(dto.getManager() == null ? "" : dto.getManager());
        order.setStartDate(dto.getStartDate());
        order.setExpectedEndDate(dto.getExpectedEndDate());
        order.setStatus(ProductionStatus.WO_PENDING);
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        order.setCreatedBy(currentUserProvider.getUserId());
        workOrderMapper.insert(order);
        workflowService.log("WORK_ORDER", order.getId(), "",
                ProductionStatus.WO_PENDING, "创建生产工单");

        if (plan != null) {
            workflowService.syncPlanStatus(plan.getId());
        }
        return detail(order.getId());
    }

    @Override
    @Transactional
    public void start(Long id) {
        WorkOrder order = requireOrder(id);
        if (!ProductionStatus.WO_PENDING.equals(order.getStatus())
                && !ProductionStatus.WO_REWORKING.equals(order.getStatus())) {
            throw new BusinessException("当前工单状态不能开始生产");
        }
        Long issueCount = issueMapper.selectCount(new LambdaQueryWrapper<MaterialIssue>()
                .eq(MaterialIssue::getWorkOrderId, order.getId()));
        if (issueCount == null || issueCount == 0) {
            throw new BusinessException("请先登记领料，再开始生产");
        }
        String from = order.getStatus();
        order.setStatus(ProductionStatus.WO_IN_PROGRESS);
        if (order.getActualStartTime() == null) {
            order.setActualStartTime(LocalDateTime.now());
        }
        workOrderMapper.updateById(order);
        workflowService.log("WORK_ORDER", order.getId(), from,
                ProductionStatus.WO_IN_PROGRESS, "开始生产");
        workflowService.syncPlanStatus(order.getPlanId());
    }

    @Override
    @Transactional
    public void cancel(Long id, String reason) {
        WorkOrder order = requireOrder(id);
        if (!ProductionStatus.WO_PENDING.equals(order.getStatus())) {
            throw new BusinessException("只有待生产状态的工单可以取消");
        }
        String from = order.getStatus();
        order.setStatus(ProductionStatus.WO_CANCELLED);
        order.setCancelReason(reason == null ? "" : reason);
        workOrderMapper.updateById(order);
        workflowService.log("WORK_ORDER", order.getId(), from,
                ProductionStatus.WO_CANCELLED, "取消工单：" + order.getCancelReason());
        workflowService.syncPlanStatus(order.getPlanId());
    }

    @Override
    public List<ProductionStatusLogVO> statusLogs(Long id) {
        requireOrder(id);
        return workflowService.logs("WORK_ORDER", id);
    }

    @Override
    @Transactional
    public MaterialIssueVO createIssue(Long workOrderId, MaterialIssueCreateDTO dto) {
        WorkOrder order = requireOrder(workOrderId);
        if (!ProductionStatus.WO_PENDING.equals(order.getStatus())
                && !ProductionStatus.WO_IN_PROGRESS.equals(order.getStatus())
                && !ProductionStatus.WO_REWORKING.equals(order.getStatus())) {
            throw new BusinessException("当前工单状态不能领料");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("请至少添加一条领料明细");
        }

        List<MaterialIssueItemDTO> sorted = new ArrayList<>(dto.getItems());
        sorted.sort(Comparator.comparing(MaterialIssueItemDTO::getMaterialBatchId));

        MaterialIssue issue = new MaterialIssue();
        issue.setIssueNo(generateIssueNo());
        issue.setWorkOrderId(workOrderId);
        issue.setIssueDate(dto.getIssueDate());
        issue.setIssuer(dto.getIssuer() == null ? "" : dto.getIssuer());
        issue.setStatus("ISSUED");
        issue.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        issue.setCreatedBy(currentUserProvider.getUserId());
        issueMapper.insert(issue);

        for (MaterialIssueItemDTO itemDto : sorted) {
            MaterialBatch batch = materialBatchMapper.selectById(itemDto.getMaterialBatchId());
            if (batch == null) {
                throw new BusinessException("原材料批次不存在");
            }

            MaterialIssueItem item = new MaterialIssueItem();
            item.setIssueId(issue.getId());
            item.setMaterialBatchId(batch.getId());
            item.setMaterialId(batch.getMaterialId());
            item.setQuantity(itemDto.getQuantity());
            item.setUnit(batch.getUnit());
            item.setRemark(itemDto.getRemark() == null ? "" : itemDto.getRemark());
            issueItemMapper.insert(item);

            // 扣减库存并生成库存流水；库存不足时整个领料事务回滚
            inventoryService.issueMaterial(batch.getId(), itemDto.getQuantity(),
                    issue.getIssueNo(), "生产领料");
        }
        return loadIssuesForOrder(workOrderId).stream()
                .filter(vo -> vo.getId().equals(issue.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("领料单创建失败"));
    }

    @Override
    public Page<MaterialIssueVO> listIssues(Integer page, Integer size,
                                            String keyword, Long workOrderId) {
        Page<MaterialIssue> issuePage = issueMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MaterialIssue>()
                        .like(StringUtils.hasText(keyword), MaterialIssue::getIssueNo, keyword)
                        .eq(workOrderId != null, MaterialIssue::getWorkOrderId, workOrderId)
                        .orderByDesc(MaterialIssue::getCreatedAt));
        List<MaterialIssue> records = issuePage.getRecords();
        Map<Long, WorkOrder> orders = loadOrders(
                records.stream().map(MaterialIssue::getWorkOrderId).toList());
        List<MaterialIssueVO> vos = records.stream()
                .map(issue -> toIssueVO(issue,
                        orders.get(issue.getWorkOrderId()) == null
                                ? null : orders.get(issue.getWorkOrderId()).getWorkOrderNo()))
                .toList();
        Page<MaterialIssueVO> result = new Page<>(
                issuePage.getCurrent(), issuePage.getSize(), issuePage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public ProductionProgressVO addProgress(Long workOrderId, ProductionProgressDTO dto) {
        WorkOrder order = workOrderMapper.selectForUpdate(workOrderId);
        if (order == null) {
            throw new BusinessException("生产工单不存在");
        }
        if (!ProductionStatus.WO_IN_PROGRESS.equals(order.getStatus())) {
            throw new BusinessException("请先开始生产再登记进度");
        }
        BigDecimal last = order.getCompletedQuantity() == null
                ? BigDecimal.ZERO : order.getCompletedQuantity();
        if (dto.getCompletedQuantity().compareTo(last) < 0) {
            throw new BusinessException("累计完成数量不能小于上次登记数量");
        }
        if (dto.getCompletedQuantity().compareTo(order.getPlannedQuantity()) > 0) {
            throw new BusinessException("完成数量不能超过计划生产数量");
        }

        BigDecimal percent = dto.getProgressPercent();
        if (percent == null) {
            percent = dto.getCompletedQuantity()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(order.getPlannedQuantity(), 2, RoundingMode.HALF_UP);
        }

        ProductionProgress progress = new ProductionProgress();
        progress.setWorkOrderId(workOrderId);
        progress.setProgressDate(dto.getProgressDate());
        progress.setProcessName(dto.getProcessName() == null ? "" : dto.getProcessName());
        progress.setCompletedQuantity(dto.getCompletedQuantity());
        progress.setProgressPercent(percent);
        progress.setRecorder(dto.getRecorder() == null ? "" : dto.getRecorder());
        progress.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        progressMapper.insert(progress);

        order.setCompletedQuantity(dto.getCompletedQuantity());
        if (dto.getCompletedQuantity().compareTo(order.getPlannedQuantity()) >= 0) {
            String from = order.getStatus();
            order.setStatus(ProductionStatus.WO_PENDING_QC);
            if (order.getActualEndTime() == null) {
                order.setActualEndTime(LocalDateTime.now());
            }
            workOrderMapper.updateById(order);
            workflowService.log("WORK_ORDER", order.getId(), from,
                    ProductionStatus.WO_PENDING_QC, "报工完成，转入待质检");
        } else {
            workOrderMapper.updateById(order);
        }
        workflowService.syncPlanStatus(order.getPlanId());
        return toProgressVO(progress, order.getWorkOrderNo());
    }

    @Override
    @Transactional
    public FinishedInspectionVO addInspection(Long workOrderId, FinishedInspectionDTO dto) {
        WorkOrder order = workOrderMapper.selectForUpdate(workOrderId);
        if (order == null) {
            throw new BusinessException("生产工单不存在");
        }
        if (!ProductionStatus.WO_PENDING_QC.equals(order.getStatus())
                && !ProductionStatus.WO_REWORKING.equals(order.getStatus())) {
            throw new BusinessException("只有待质检或返工中的工单可以进行成品质检");
        }

        BigDecimal total = dto.getQualifiedQuantity().add(dto.getUnqualifiedQuantity());
        if (total.compareTo(dto.getInspectedQuantity()) != 0) {
            throw new BusinessException("合格数量与不合格数量之和必须等于送检数量");
        }
        List<FinishedInspection> existing = inspectionMapper.selectList(
                new LambdaQueryWrapper<FinishedInspection>()
                        .eq(FinishedInspection::getWorkOrderId, workOrderId));
        BigDecimal inspectedAlready = existing.stream()
                .map(FinishedInspection::getInspectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (inspectedAlready.add(dto.getInspectedQuantity())
                .compareTo(order.getCompletedQuantity()) > 0) {
            throw new BusinessException("送检数量不能超过工单已完成数量");
        }

        String result;
        if (dto.getUnqualifiedQuantity().compareTo(BigDecimal.ZERO) == 0) {
            result = ProductionStatus.RESULT_QUALIFIED;
        } else if (dto.getQualifiedQuantity().compareTo(BigDecimal.ZERO) == 0) {
            result = ProductionStatus.RESULT_UNQUALIFIED;
        } else {
            result = ProductionStatus.RESULT_PARTIAL;
        }

        FinishedInspection inspection = new FinishedInspection();
        inspection.setInspectionNo(generateInspectionNo());
        inspection.setWorkOrderId(workOrderId);
        inspection.setProductId(order.getProductId());
        inspection.setSkuId(order.getSkuId());
        inspection.setInspectedQuantity(dto.getInspectedQuantity());
        inspection.setQualifiedQuantity(dto.getQualifiedQuantity());
        inspection.setUnqualifiedQuantity(dto.getUnqualifiedQuantity());
        inspection.setResult(result);
        inspection.setInspector(dto.getInspector() == null ? "" : dto.getInspector());
        inspection.setInspectionTime(dto.getInspectionTime() == null
                ? LocalDateTime.now() : dto.getInspectionTime());
        inspection.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        inspectionMapper.insert(inspection);

        BigDecimal qualifiedTotal = existing.stream()
                .map(FinishedInspection::getQualifiedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(dto.getQualifiedQuantity());
        BigDecimal inspectedTotal = inspectedAlready.add(dto.getInspectedQuantity());
        BigDecimal unqualifiedTotal = existing.stream()
                .map(FinishedInspection::getUnqualifiedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(dto.getUnqualifiedQuantity());

        String from = order.getStatus();
        String nextStatus;
        if (inspectedTotal.compareTo(order.getCompletedQuantity()) < 0) {
            nextStatus = ProductionStatus.WO_PENDING_QC;
        } else if (unqualifiedTotal.compareTo(BigDecimal.ZERO) == 0) {
            nextStatus = ProductionStatus.WO_COMPLETED;
        } else {
            nextStatus = ProductionStatus.WO_REWORKING;
        }

        order.setQualifiedQuantity(qualifiedTotal);
        order.setStatus(nextStatus);
        if (ProductionStatus.WO_COMPLETED.equals(nextStatus)
                && order.getActualEndTime() == null) {
            order.setActualEndTime(LocalDateTime.now());
        }
        workOrderMapper.updateById(order);
        workflowService.log("WORK_ORDER", order.getId(), from, nextStatus,
                ProductionStatus.WO_COMPLETED.equals(nextStatus)
                        ? "成品质检完成，全部合格" : "成品质检完成");
        workflowService.syncPlanStatus(order.getPlanId());

        return toInspectionVO(inspection, order.getWorkOrderNo(),
                productMapper.selectById(order.getProductId()),
                skuMapper.selectById(order.getSkuId()));
    }

    @Override
    public Page<FinishedInspectionVO> listInspections(Integer page, Integer size, String keyword) {
        Page<FinishedInspection> inspectionPage = inspectionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<FinishedInspection>()
                        .like(StringUtils.hasText(keyword),
                                FinishedInspection::getInspectionNo, keyword)
                        .orderByDesc(FinishedInspection::getCreatedAt));
        List<FinishedInspection> records = inspectionPage.getRecords();
        Map<Long, WorkOrder> orders = loadOrders(
                records.stream().map(FinishedInspection::getWorkOrderId).toList());
        Map<Long, Product> products = loadProducts(
                records.stream().map(FinishedInspection::getProductId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                records.stream().map(FinishedInspection::getSkuId).toList());
        List<FinishedInspectionVO> vos = records.stream()
                .map(item -> toInspectionVO(item,
                        orders.get(item.getWorkOrderId()) == null
                                ? null : orders.get(item.getWorkOrderId()).getWorkOrderNo(),
                        products.get(item.getProductId()),
                        skus.get(item.getSkuId())))
                .toList();
        Page<FinishedInspectionVO> result = new Page<>(
                inspectionPage.getCurrent(), inspectionPage.getSize(), inspectionPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public QualityDispositionVO addDisposition(QualityDispositionDTO dto) {
        FinishedInspection inspection = inspectionMapper.selectById(dto.getInspectionId());
        if (inspection == null) {
            throw new BusinessException("成品质检记录不存在");
        }
        String type = dto.getType() == null ? "" : dto.getType().toUpperCase();
        if (!DISPOSITION_TYPES.contains(type)) {
            throw new BusinessException("处理类型不合法");
        }
        BigDecimal disposed = dispositionMapper.selectList(
                        new LambdaQueryWrapper<QualityDisposition>()
                                .eq(QualityDisposition::getInspectionId, inspection.getId()))
                .stream().map(QualityDisposition::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disposed.add(dto.getQuantity())
                .compareTo(inspection.getUnqualifiedQuantity()) > 0) {
            throw new BusinessException("处理数量不能超过该次质检的不合格数量");
        }

        QualityDisposition disposition = new QualityDisposition();
        disposition.setInspectionId(inspection.getId());
        disposition.setWorkOrderId(inspection.getWorkOrderId());
        disposition.setType(type);
        disposition.setQuantity(dto.getQuantity());
        disposition.setReason(dto.getReason() == null ? "" : dto.getReason());
        disposition.setHandler(dto.getHandler() == null ? "" : dto.getHandler());
        disposition.setHandledAt(LocalDateTime.now());
        disposition.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        dispositionMapper.insert(disposition);

        WorkOrder order = requireOrder(inspection.getWorkOrderId());
        BigDecimal disposedTotal = disposed.add(dto.getQuantity());
        BigDecimal inspectedTotal = inspectionMapper.selectList(
                        new LambdaQueryWrapper<FinishedInspection>()
                                .eq(FinishedInspection::getWorkOrderId, order.getId()))
                .stream().map(FinishedInspection::getInspectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String from = order.getStatus();
        String nextStatus;
        if (ProductionStatus.DISPOSITION_REWORK.equals(type)) {
            nextStatus = ProductionStatus.WO_REWORKING;
        } else if (ProductionStatus.DISPOSITION_RECHECK.equals(type)) {
            nextStatus = ProductionStatus.WO_PENDING_QC;
        } else if (disposedTotal.compareTo(inspection.getUnqualifiedQuantity()) >= 0) {
            nextStatus = inspectedTotal.compareTo(order.getCompletedQuantity()) >= 0
                    ? ProductionStatus.WO_COMPLETED : ProductionStatus.WO_PENDING_QC;
        } else {
            nextStatus = ProductionStatus.WO_REWORKING;
        }

        order.setStatus(nextStatus);
        if (ProductionStatus.WO_COMPLETED.equals(nextStatus)
                && order.getActualEndTime() == null) {
            order.setActualEndTime(LocalDateTime.now());
        }
        workOrderMapper.updateById(order);
        workflowService.log("WORK_ORDER", order.getId(), from, nextStatus,
                "不合格品处理：" + type);
        workflowService.syncPlanStatus(order.getPlanId());

        return toDispositionVO(disposition);
    }

    @Override
    @Transactional
    public FinishedBatchVO createBatch(Long workOrderId, FinishedBatchCreateDTO dto) {
        WorkOrder order = requireOrder(workOrderId);
        BigDecimal qualified = order.getQualifiedQuantity() == null
                ? BigDecimal.ZERO : order.getQualifiedQuantity();
        BigDecimal batched = batchMapper.selectList(new LambdaQueryWrapper<FinishedBatch>()
                        .eq(FinishedBatch::getWorkOrderId, workOrderId))
                .stream().map(FinishedBatch::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = qualified.subtract(batched);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("没有可生成成品批次的合格数量");
        }

        BigDecimal batchQuantity = dto.getQuantity() == null ? remaining : dto.getQuantity();
        if (batchQuantity.compareTo(BigDecimal.ZERO) <= 0
                || batchQuantity.compareTo(remaining) > 0) {
            throw new BusinessException("本次批次数量必须在1到剩余合格数量 "
                    + remaining.stripTrailingZeros().toPlainString() + " 之间");
        }

        FinishedBatch batch = new FinishedBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setWorkOrderId(workOrderId);
        batch.setProductId(order.getProductId());
        batch.setSkuId(order.getSkuId());
        batch.setQuantity(batchQuantity);
        batch.setStatus(ProductionStatus.BATCH_PENDING_INBOUND);
        batch.setProductionDate(dto.getProductionDate() == null
                ? LocalDate.now() : dto.getProductionDate());
        batch.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        batchMapper.insert(batch);

        // 原材料使用量按本次批次数量占合格数量的比例分摊
        BigDecimal ratio = qualified.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : batchQuantity.divide(qualified, 6, RoundingMode.HALF_UP);
        Map<Long, BigDecimal> materialUsage = new HashMap<>();
        List<MaterialIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<MaterialIssue>()
                .eq(MaterialIssue::getWorkOrderId, workOrderId));
        if (!issues.isEmpty()) {
            List<Long> issueIds = issues.stream().map(MaterialIssue::getId).toList();
            List<MaterialIssueItem> items = issueItemMapper.selectList(
                    new LambdaQueryWrapper<MaterialIssueItem>()
                            .in(MaterialIssueItem::getIssueId, issueIds));
            for (MaterialIssueItem item : items) {
                materialUsage.merge(item.getMaterialBatchId(), item.getQuantity(), BigDecimal::add);
            }
        }
        for (Map.Entry<Long, BigDecimal> entry : materialUsage.entrySet()) {
            FinishedBatchMaterial relation = new FinishedBatchMaterial();
            relation.setFinishedBatchId(batch.getId());
            relation.setMaterialBatchId(entry.getKey());
            relation.setQuantity(entry.getValue().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP));
            batchMaterialMapper.insert(relation);
        }

        return toBatchVO(batch, order.getWorkOrderNo(),
                productMapper.selectById(order.getProductId()),
                skuMapper.selectById(order.getSkuId()));
    }

    @Override
    public Page<FinishedBatchVO> listBatches(Integer page, Integer size,
                                             String keyword, String status) {
        Page<FinishedBatch> batchPage = batchMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FinishedBatch>()
                        .like(StringUtils.hasText(keyword), FinishedBatch::getBatchNo, keyword)
                        .eq(StringUtils.hasText(status), FinishedBatch::getStatus, status)
                        .orderByDesc(FinishedBatch::getCreatedAt));
        List<FinishedBatch> records = batchPage.getRecords();
        Map<Long, WorkOrder> orders = loadOrders(
                records.stream().map(FinishedBatch::getWorkOrderId).toList());
        Map<Long, Product> products = loadProducts(
                records.stream().map(FinishedBatch::getProductId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                records.stream().map(FinishedBatch::getSkuId).toList());
        List<FinishedBatchVO> vos = records.stream()
                .map(batch -> toBatchVO(batch,
                        orders.get(batch.getWorkOrderId()) == null
                                ? null : orders.get(batch.getWorkOrderId()).getWorkOrderNo(),
                        products.get(batch.getProductId()),
                        skus.get(batch.getSkuId())))
                .toList();
        Page<FinishedBatchVO> result = new Page<>(
                batchPage.getCurrent(), batchPage.getSize(), batchPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    // ==========================================================
    // 查询与装配
    // ==========================================================

    private List<MaterialIssueVO> loadIssuesForOrder(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        return issueMapper.selectList(new LambdaQueryWrapper<MaterialIssue>()
                        .eq(MaterialIssue::getWorkOrderId, workOrderId)
                        .orderByAsc(MaterialIssue::getId))
                .stream()
                .map(issue -> toIssueVO(issue, order == null ? null : order.getWorkOrderNo()))
                .toList();
    }

    private MaterialIssueVO toIssueVO(MaterialIssue issue, String workOrderNo) {
        List<MaterialIssueItem> items = issueItemMapper.selectList(
                new LambdaQueryWrapper<MaterialIssueItem>()
                        .eq(MaterialIssueItem::getIssueId, issue.getId())
                        .orderByAsc(MaterialIssueItem::getId));
        Map<Long, MaterialBatch> batches = loadBatches(
                items.stream().map(MaterialIssueItem::getMaterialBatchId).toList());
        Map<Long, Material> materials = loadMaterials(
                items.stream().map(MaterialIssueItem::getMaterialId).toList());
        List<MaterialIssueItemVO> itemVOs = items.stream().map(item -> {
            MaterialBatch batch = batches.get(item.getMaterialBatchId());
            Material material = materials.get(item.getMaterialId());
            return MaterialIssueItemVO.builder()
                    .id(item.getId())
                    .materialBatchId(item.getMaterialBatchId())
                    .batchNo(batch == null ? null : batch.getBatchNo())
                    .materialId(item.getMaterialId())
                    .materialCode(material == null ? null : material.getMaterialCode())
                    .materialName(material == null ? null : material.getName())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .remark(item.getRemark())
                    .build();
        }).toList();
        return MaterialIssueVO.builder()
                .id(issue.getId())
                .issueNo(issue.getIssueNo())
                .workOrderId(issue.getWorkOrderId())
                .workOrderNo(workOrderNo)
                .issueDate(issue.getIssueDate())
                .issuer(issue.getIssuer())
                .status(issue.getStatus())
                .remark(issue.getRemark())
                .createdAt(issue.getCreatedAt())
                .items(itemVOs)
                .build();
    }

    private List<ProductionProgressVO> loadProgressForOrder(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        return progressMapper.selectList(new LambdaQueryWrapper<ProductionProgress>()
                        .eq(ProductionProgress::getWorkOrderId, workOrderId)
                        .orderByAsc(ProductionProgress::getProgressDate)
                        .orderByAsc(ProductionProgress::getId))
                .stream()
                .map(item -> toProgressVO(item, order == null ? null : order.getWorkOrderNo()))
                .toList();
    }

    private ProductionProgressVO toProgressVO(ProductionProgress progress, String workOrderNo) {
        return ProductionProgressVO.builder()
                .id(progress.getId())
                .workOrderId(progress.getWorkOrderId())
                .workOrderNo(workOrderNo)
                .progressDate(progress.getProgressDate())
                .processName(progress.getProcessName())
                .completedQuantity(progress.getCompletedQuantity())
                .progressPercent(progress.getProgressPercent())
                .recorder(progress.getRecorder())
                .remark(progress.getRemark())
                .createdAt(progress.getCreatedAt())
                .build();
    }

    private List<FinishedInspectionVO> loadInspectionsForOrder(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            return List.of();
        }
        Product product = productMapper.selectById(order.getProductId());
        ProductSku sku = skuMapper.selectById(order.getSkuId());
        return inspectionMapper.selectList(new LambdaQueryWrapper<FinishedInspection>()
                        .eq(FinishedInspection::getWorkOrderId, workOrderId)
                        .orderByAsc(FinishedInspection::getId))
                .stream()
                .map(item -> toInspectionVO(item, order.getWorkOrderNo(), product, sku))
                .toList();
    }

    private FinishedInspectionVO toInspectionVO(FinishedInspection inspection, String workOrderNo,
                                                Product product, ProductSku sku) {
        return FinishedInspectionVO.builder()
                .id(inspection.getId())
                .inspectionNo(inspection.getInspectionNo())
                .workOrderId(inspection.getWorkOrderId())
                .workOrderNo(workOrderNo)
                .productId(inspection.getProductId())
                .productName(product == null ? null : product.getName())
                .skuId(inspection.getSkuId())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .inspectedQuantity(inspection.getInspectedQuantity())
                .qualifiedQuantity(inspection.getQualifiedQuantity())
                .unqualifiedQuantity(inspection.getUnqualifiedQuantity())
                .result(inspection.getResult())
                .inspector(inspection.getInspector())
                .inspectionTime(inspection.getInspectionTime())
                .remark(inspection.getRemark())
                .createdAt(inspection.getCreatedAt())
                .build();
    }

    private QualityDispositionVO toDispositionVO(QualityDisposition disposition) {
        return QualityDispositionVO.builder()
                .id(disposition.getId())
                .inspectionId(disposition.getInspectionId())
                .workOrderId(disposition.getWorkOrderId())
                .type(disposition.getType())
                .quantity(disposition.getQuantity())
                .reason(disposition.getReason())
                .handler(disposition.getHandler())
                .handledAt(disposition.getHandledAt())
                .remark(disposition.getRemark())
                .createdAt(disposition.getCreatedAt())
                .build();
    }

    private List<FinishedBatchVO> loadBatchesForOrder(Long workOrderId) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            return List.of();
        }
        Product product = productMapper.selectById(order.getProductId());
        ProductSku sku = skuMapper.selectById(order.getSkuId());
        return batchMapper.selectList(new LambdaQueryWrapper<FinishedBatch>()
                        .eq(FinishedBatch::getWorkOrderId, workOrderId)
                        .orderByAsc(FinishedBatch::getId))
                .stream()
                .map(batch -> toBatchVO(batch, order.getWorkOrderNo(), product, sku))
                .toList();
    }

    private FinishedBatchVO toBatchVO(FinishedBatch batch, String workOrderNo,
                                      Product product, ProductSku sku) {
        List<FinishedBatchMaterial> relations = batchMaterialMapper.selectList(
                new LambdaQueryWrapper<FinishedBatchMaterial>()
                        .eq(FinishedBatchMaterial::getFinishedBatchId, batch.getId()));
        Map<Long, MaterialBatch> batches = loadBatches(
                relations.stream().map(FinishedBatchMaterial::getMaterialBatchId).toList());
        Map<Long, Material> materials = loadMaterials(
                batches.values().stream().map(MaterialBatch::getMaterialId).toList());
        List<FinishedBatchMaterialVO> materialVOs = relations.stream().map(relation -> {
            MaterialBatch materialBatch = batches.get(relation.getMaterialBatchId());
            Material material = materialBatch == null
                    ? null : materials.get(materialBatch.getMaterialId());
            return FinishedBatchMaterialVO.builder()
                    .materialBatchId(relation.getMaterialBatchId())
                    .batchNo(materialBatch == null ? null : materialBatch.getBatchNo())
                    .materialCode(material == null ? null : material.getMaterialCode())
                    .materialName(material == null ? null : material.getName())
                    .quantity(relation.getQuantity())
                    .unit(materialBatch == null ? null : materialBatch.getUnit())
                    .build();
        }).toList();
        return FinishedBatchVO.builder()
                .id(batch.getId())
                .batchNo(batch.getBatchNo())
                .workOrderId(batch.getWorkOrderId())
                .workOrderNo(workOrderNo)
                .productId(batch.getProductId())
                .productName(product == null ? null : product.getName())
                .skuId(batch.getSkuId())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .quantity(batch.getQuantity())
                .status(batch.getStatus())
                .productionDate(batch.getProductionDate())
                .warehouseId(batch.getWarehouseId())
                .remark(batch.getRemark())
                .createdAt(batch.getCreatedAt())
                .materials(materialVOs)
                .build();
    }

    private WorkOrderVO baseVO(WorkOrder order, Product product, ProductSku sku,
                               ProductionPlan plan) {
        return WorkOrderVO.builder()
                .id(order.getId())
                .workOrderNo(order.getWorkOrderNo())
                .planId(order.getPlanId())
                .planNo(plan == null ? null : plan.getPlanNo())
                .productId(order.getProductId())
                .productName(product == null ? null : product.getName())
                .skuId(order.getSkuId())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .plannedQuantity(order.getPlannedQuantity())
                .completedQuantity(order.getCompletedQuantity())
                .qualifiedQuantity(order.getQualifiedQuantity())
                .manager(order.getManager())
                .startDate(order.getStartDate())
                .expectedEndDate(order.getExpectedEndDate())
                .status(order.getStatus())
                .remark(order.getRemark())
                .actualStartTime(order.getActualStartTime())
                .actualEndTime(order.getActualEndTime())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private WorkOrder requireOrder(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("生产工单不存在");
        }
        return order;
    }

    private void validateProductSku(Product product, ProductSku sku,
                                    Long productId, Long skuId) {
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("产品不存在或已停用");
        }
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException("SKU不存在或已停用");
        }
        if (!productId.equals(sku.getProductId())) {
            throw new BusinessException("SKU不属于所选产品");
        }
    }

    private Map<Long, BigDecimal> loadInspectedQuantities(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return inspectionMapper.selectList(new LambdaQueryWrapper<FinishedInspection>()
                        .in(FinishedInspection::getWorkOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(FinishedInspection::getWorkOrderId,
                        Collectors.mapping(FinishedInspection::getInspectedQuantity,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private Map<Long, String> loadCurrentProcesses(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductionProgress> list = progressMapper.selectList(
                new LambdaQueryWrapper<ProductionProgress>()
                        .in(ProductionProgress::getWorkOrderId, orderIds));
        Map<Long, ProductionProgress> latest = new HashMap<>();
        for (ProductionProgress item : list) {
            ProductionProgress current = latest.get(item.getWorkOrderId());
            if (current == null
                    || item.getProgressDate().isAfter(current.getProgressDate())
                    || (item.getProgressDate().isEqual(current.getProgressDate())
                        && item.getId() > current.getId())) {
                latest.put(item.getWorkOrderId(), item);
            }
        }
        Map<Long, String> result = new HashMap<>();
        latest.forEach((orderId, progress) ->
                result.put(orderId, progress.getProcessName()));
        return result;
    }

    private Map<Long, BigDecimal> loadBatchedQuantities(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchMapper.selectList(new LambdaQueryWrapper<FinishedBatch>()
                        .in(FinishedBatch::getWorkOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(FinishedBatch::getWorkOrderId,
                        Collectors.mapping(FinishedBatch::getQuantity,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private int safeInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private Map<Long, Product> loadProducts(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private Map<Long, ProductSku> loadSkus(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return skuMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));
    }

    private Map<Long, ProductionPlan> loadPlans(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return planMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(ProductionPlan::getId, plan -> plan));
    }

    private Map<Long, WorkOrder> loadOrders(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(WorkOrder::getId, order -> order));
    }

    private Map<Long, MaterialBatch> loadBatches(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialBatchMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MaterialBatch::getId, batch -> batch));
    }

    private Map<Long, Material> loadMaterials(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private String generateWorkOrderNo() {
        String prefix = "WO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .likeRight(WorkOrder::getWorkOrderNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateIssueNo() {
        String prefix = "MI-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = issueMapper.selectCount(new LambdaQueryWrapper<MaterialIssue>()
                .likeRight(MaterialIssue::getIssueNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateInspectionNo() {
        String prefix = "FI-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = inspectionMapper.selectCount(new LambdaQueryWrapper<FinishedInspection>()
                .likeRight(FinishedInspection::getInspectionNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateBatchNo() {
        String prefix = "FB-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = batchMapper.selectCount(new LambdaQueryWrapper<FinishedBatch>()
                .likeRight(FinishedBatch::getBatchNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}