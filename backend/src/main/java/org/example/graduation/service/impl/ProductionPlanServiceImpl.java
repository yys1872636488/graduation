package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.dto.ProductionPlanSaveDTO;
import org.example.graduation.entity.Product;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.entity.ProductionPlan;
import org.example.graduation.entity.WorkOrder;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.mapper.ProductSkuMapper;
import org.example.graduation.mapper.ProductionPlanMapper;
import org.example.graduation.mapper.WorkOrderMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.ProductionPlanService;
import org.example.graduation.service.ProductionWorkflowService;
import org.example.graduation.service.WorkOrderService;
import org.example.graduation.vo.ProductionPlanVO;
import org.example.graduation.vo.ProductionStatusLogVO;
import org.example.graduation.vo.WorkOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionPlanServiceImpl implements ProductionPlanService {

    private final ProductionPlanMapper planMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProductionWorkflowService workflowService;
    private final WorkOrderService workOrderService;

    @Override
    public Page<ProductionPlanVO> list(Integer page, Integer size, String keyword, String status) {
        Page<ProductionPlan> planPage = planMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductionPlan>()
                        .like(StringUtils.hasText(keyword), ProductionPlan::getPlanNo, keyword)
                        .eq(StringUtils.hasText(status), ProductionPlan::getStatus, status)
                        .orderByDesc(ProductionPlan::getCreatedAt));
        List<ProductionPlan> records = planPage.getRecords();
        Map<Long, Product> products = loadProducts(
                records.stream().map(ProductionPlan::getProductId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                records.stream().map(ProductionPlan::getSkuId).toList());
        Map<Long, List<WorkOrder>> ordersByPlan = loadWorkOrders(
                records.stream().map(ProductionPlan::getId).toList());

        List<ProductionPlanVO> vos = records.stream().map(plan -> {
            List<WorkOrder> orders = ordersByPlan.getOrDefault(plan.getId(), List.of())
                    .stream()
                    .filter(order -> !ProductionStatus.WO_CANCELLED.equals(order.getStatus()))
                    .toList();
            BigDecimal workOrderQuantity = orders.stream()
                    .map(WorkOrder::getPlannedQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = plan.getPlannedQuantity().subtract(workOrderQuantity)
                    .max(BigDecimal.ZERO);
            return toVO(plan, products.get(plan.getProductId()), skus.get(plan.getSkuId()),
                    orders.size(), workOrderQuantity, remaining);
        }).toList();

        Page<ProductionPlanVO> result = new Page<>(
                planPage.getCurrent(), planPage.getSize(), planPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public ProductionPlanVO create(ProductionPlanSaveDTO dto) {
        validateProductSku(dto.getProductId(), dto.getSkuId());
        validateDates(dto);
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo(generatePlanNo());
        apply(plan, dto);
        plan.setStatus(ProductionStatus.PLAN_DRAFT);
        plan.setCreatedBy(currentUserProvider.getUserId());
        planMapper.insert(plan);
        workflowService.log("PLAN", plan.getId(), "", ProductionStatus.PLAN_DRAFT, "创建生产计划");
        return toVO(plan, productMapper.selectById(plan.getProductId()),
                skuMapper.selectById(plan.getSkuId()), 0, BigDecimal.ZERO,
                plan.getPlannedQuantity());
    }

    @Override
    public ProductionPlanVO update(Long id, ProductionPlanSaveDTO dto) {
        ProductionPlan plan = requirePlan(id);
        if (!ProductionStatus.PLAN_DRAFT.equals(plan.getStatus())) {
            throw new BusinessException("只有草稿状态的计划可以编辑");
        }
        validateProductSku(dto.getProductId(), dto.getSkuId());
        validateDates(dto);
        apply(plan, dto);
        planMapper.updateById(plan);
        return toVO(plan, productMapper.selectById(plan.getProductId()),
                skuMapper.selectById(plan.getSkuId()), 0, BigDecimal.ZERO,
                plan.getPlannedQuantity());
    }

    @Override
    @Transactional
    public WorkOrderVO release(Long id, BigDecimal quantity) {
        ProductionPlan plan = requirePlan(id);
        if (!ProductionStatus.PLAN_DRAFT.equals(plan.getStatus())
                && !ProductionStatus.PLAN_RELEASED.equals(plan.getStatus())
                && !ProductionStatus.PLAN_IN_PROGRESS.equals(plan.getStatus())) {
            throw new BusinessException("只有未完成的计划可以生成工单");
        }

        List<WorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getPlanId, id));
        BigDecimal consumed = orders.stream()
                .filter(order -> !ProductionStatus.WO_CANCELLED.equals(order.getStatus()))
                .map(WorkOrder::getPlannedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = plan.getPlannedQuantity().subtract(consumed);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("计划数量已全部生成工单");
        }
        BigDecimal releaseQuantity = quantity == null ? remaining : quantity;
        if (releaseQuantity.compareTo(BigDecimal.ZERO) <= 0
                || releaseQuantity.compareTo(remaining) > 0) {
            throw new BusinessException("本次工单数量必须在1到剩余数量 "
                    + remaining.stripTrailingZeros().toPlainString() + " 之间");
        }

        WorkOrder order = new WorkOrder();
        order.setWorkOrderNo(generateWorkOrderNo());
        order.setPlanId(plan.getId());
        order.setProductId(plan.getProductId());
        order.setSkuId(plan.getSkuId());
        order.setPlannedQuantity(releaseQuantity);
        order.setCompletedQuantity(BigDecimal.ZERO);
        order.setQualifiedQuantity(BigDecimal.ZERO);
        order.setManager("");
        order.setStartDate(plan.getStartDate());
        order.setExpectedEndDate(plan.getEndDate());
        order.setStatus(ProductionStatus.WO_PENDING);
        order.setRemark("");
        order.setCreatedBy(currentUserProvider.getUserId());
        workOrderMapper.insert(order);
        workflowService.log("WORK_ORDER", order.getId(), "",
                ProductionStatus.WO_PENDING, "由生产计划下达生成");

        if (ProductionStatus.PLAN_DRAFT.equals(plan.getStatus())) {
            String from = plan.getStatus();
            plan.setStatus(ProductionStatus.PLAN_RELEASED);
            plan.setReleasedAt(LocalDateTime.now());
            planMapper.updateById(plan);
            workflowService.log("PLAN", plan.getId(), from,
                    ProductionStatus.PLAN_RELEASED, "计划下达并生成工单");
        }
        workflowService.syncPlanStatus(plan.getId());
        return workOrderService.detail(order.getId());
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        ProductionPlan plan = requirePlan(id);
        if (!ProductionStatus.PLAN_DRAFT.equals(plan.getStatus())
                && !ProductionStatus.PLAN_RELEASED.equals(plan.getStatus())) {
            throw new BusinessException("当前状态的计划不能取消");
        }
        List<WorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getPlanId, id));
        boolean hasStarted = orders.stream().anyMatch(order ->
                !ProductionStatus.WO_PENDING.equals(order.getStatus())
                        && !ProductionStatus.WO_CANCELLED.equals(order.getStatus()));
        if (hasStarted) {
            throw new BusinessException("计划下已有开工或完成的工单，不能取消");
        }

        for (WorkOrder order : orders) {
            if (ProductionStatus.WO_PENDING.equals(order.getStatus())) {
                order.setStatus(ProductionStatus.WO_CANCELLED);
                order.setCancelReason("计划取消");
                workOrderMapper.updateById(order);
                workflowService.log("WORK_ORDER", order.getId(),
                        ProductionStatus.WO_PENDING, ProductionStatus.WO_CANCELLED,
                        "计划取消，工单同步取消");
            }
        }

        String from = plan.getStatus();
        plan.setStatus(ProductionStatus.PLAN_CANCELLED);
        plan.setCompletedAt(null);
        planMapper.updateById(plan);
        workflowService.log("PLAN", plan.getId(), from,
                ProductionStatus.PLAN_CANCELLED, "取消生产计划");
    }

    @Override
    public List<ProductionStatusLogVO> statusLogs(Long id) {
        requirePlan(id);
        return workflowService.logs("PLAN", id);
    }

    private void apply(ProductionPlan plan, ProductionPlanSaveDTO dto) {
        plan.setProductId(dto.getProductId());
        plan.setSkuId(dto.getSkuId());
        plan.setPlannedQuantity(dto.getPlannedQuantity());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "NORMAL");
        plan.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
    }

    private void validateDates(ProductionPlanSaveDTO dto) {
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("计划结束日期不能早于开始日期");
        }
    }

    private void validateProductSku(Long productId, Long skuId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("产品不存在或已停用");
        }
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException("SKU不存在或已停用");
        }
        if (!productId.equals(sku.getProductId())) {
            throw new BusinessException("SKU不属于所选产品");
        }
    }

    private ProductionPlan requirePlan(Long id) {
        ProductionPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("生产计划不存在");
        }
        return plan;
    }

    private ProductionPlanVO toVO(ProductionPlan plan, Product product, ProductSku sku,
                                  int workOrderCount, BigDecimal workOrderQuantity,
                                  BigDecimal remainingQuantity) {
        return ProductionPlanVO.builder()
                .id(plan.getId())
                .planNo(plan.getPlanNo())
                .productId(plan.getProductId())
                .productName(product == null ? null : product.getName())
                .skuId(plan.getSkuId())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .plannedQuantity(plan.getPlannedQuantity())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .priority(plan.getPriority())
                .status(plan.getStatus())
                .remark(plan.getRemark())
                .workOrderCount(workOrderCount)
                .workOrderQuantity(workOrderQuantity)
                .remainingQuantity(remainingQuantity)
                .releasedAt(plan.getReleasedAt())
                .completedAt(plan.getCompletedAt())
                .createdAt(plan.getCreatedAt())
                .build();
    }

    private Map<Long, Product> loadProducts(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private Map<Long, ProductSku> loadSkus(List<Long> ids) {
        List<Long> distinct = ids.stream()
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return skuMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));
    }

    private Map<Long, List<WorkOrder>> loadWorkOrders(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                        .in(WorkOrder::getPlanId, planIds))
                .stream().collect(Collectors.groupingBy(WorkOrder::getPlanId));
    }

    private String generatePlanNo() {
        String prefix = "PP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = planMapper.selectCount(new LambdaQueryWrapper<ProductionPlan>()
                .likeRight(ProductionPlan::getPlanNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateWorkOrderNo() {
        String prefix = "WO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .likeRight(WorkOrder::getWorkOrderNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}