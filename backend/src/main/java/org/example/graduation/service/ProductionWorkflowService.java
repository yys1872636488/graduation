package org.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.entity.ProductionPlan;
import org.example.graduation.entity.ProductionStatusLog;
import org.example.graduation.entity.SysUser;
import org.example.graduation.entity.WorkOrder;
import org.example.graduation.mapper.ProductionPlanMapper;
import org.example.graduation.mapper.ProductionStatusLogMapper;
import org.example.graduation.mapper.SysUserMapper;
import org.example.graduation.mapper.WorkOrderMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.vo.ProductionStatusLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产状态机公共逻辑：状态日志、计划状态汇总。
 */
@Service
@RequiredArgsConstructor
public class ProductionWorkflowService {

    private static final Set<String> ACTIVE_WORK_ORDER_STATUSES = Set.of(
            ProductionStatus.WO_IN_PROGRESS,
            ProductionStatus.WO_PENDING_QC,
            ProductionStatus.WO_REWORKING);

    private final ProductionStatusLogMapper logMapper;
    private final ProductionPlanMapper planMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 按工单状态汇总计划状态。计划状态不再由前端或单个接口直接指定。
     */
    public void syncPlanStatus(Long planId) {
        if (planId == null) {
            return;
        }
        ProductionPlan plan = planMapper.selectById(planId);
        if (plan == null || ProductionStatus.PLAN_CANCELLED.equals(plan.getStatus())) {
            return;
        }
        List<WorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getPlanId, planId));
        List<WorkOrder> active = orders.stream()
                .filter(order -> !ProductionStatus.WO_CANCELLED.equals(order.getStatus()))
                .toList();

        java.math.BigDecimal activePlanned = active.stream()
                .map(WorkOrder::getPlannedQuantity)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        boolean allActiveCompleted = !active.isEmpty() && active.stream().allMatch(order ->
                ProductionStatus.WO_COMPLETED.equals(order.getStatus()));
        boolean allPlannedReleased = activePlanned.compareTo(plan.getPlannedQuantity()) >= 0;

        String target;
        if (active.isEmpty()) {
            target = plan.getReleasedAt() != null
                    ? ProductionStatus.PLAN_RELEASED : ProductionStatus.PLAN_DRAFT;
        } else if (allActiveCompleted && allPlannedReleased) {
            target = ProductionStatus.PLAN_COMPLETED;
        } else if (active.stream().anyMatch(order ->
                !ProductionStatus.WO_PENDING.equals(order.getStatus()))) {
            target = ProductionStatus.PLAN_IN_PROGRESS;
        } else {
            target = ProductionStatus.PLAN_RELEASED;
        }

        if (!target.equals(plan.getStatus())) {
            String from = plan.getStatus();
            plan.setStatus(target);
            plan.setCompletedAt(ProductionStatus.PLAN_COMPLETED.equals(target)
                    ? LocalDateTime.now() : null);
            planMapper.updateById(plan);
            log("PLAN", planId, from, target, "计划状态随工单状态自动更新");
        }
    }

    public void log(String bizType, Long bizId, String fromStatus,
                    String toStatus, String remark) {
        if (fromStatus != null && fromStatus.equals(toStatus)) {
            return;
        }
        ProductionStatusLog log = new ProductionStatusLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setFromStatus(fromStatus == null ? "" : fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(currentUserProvider.getUserId());
        log.setRemark(remark == null ? "" : remark);
        logMapper.insert(log);
    }

    public List<ProductionStatusLogVO> logs(String bizType, Long bizId) {
        List<ProductionStatusLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<ProductionStatusLog>()
                        .eq(ProductionStatusLog::getBizType, bizType)
                        .eq(ProductionStatusLog::getBizId, bizId)
                        .orderByAsc(ProductionStatusLog::getCreatedAt)
                        .orderByAsc(ProductionStatusLog::getId));
        if (logs.isEmpty()) {
            return List.of();
        }
        List<Long> operatorIds = logs.stream().map(ProductionStatusLog::getOperatorId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> names = operatorIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(operatorIds).stream()
                        .collect(Collectors.toMap(SysUser::getId,
                                user -> user.getRealName() == null || user.getRealName().isBlank()
                                        ? user.getUsername() : user.getRealName()));
        return logs.stream().map(log -> ProductionStatusLogVO.builder()
                .id(log.getId())
                .bizType(log.getBizType())
                .bizId(log.getBizId())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .operatorId(log.getOperatorId())
                .operatorName(names.get(log.getOperatorId()))
                .remark(log.getRemark())
                .createdAt(log.getCreatedAt())
                .build()).toList();
    }
}