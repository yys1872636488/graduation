package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.InventoryConstants;
import org.example.graduation.common.ProcurementStatus;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.common.SalesStatus;
import org.example.graduation.entity.AfterSalesRecord;
import org.example.graduation.entity.FinishedBatch;
import org.example.graduation.entity.Inventory;
import org.example.graduation.entity.InspectionRecord;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.PurchaseOrder;
import org.example.graduation.entity.SalesOrder;
import org.example.graduation.entity.Shipment;
import org.example.graduation.entity.WorkOrder;
import org.example.graduation.mapper.AfterSalesRecordMapper;
import org.example.graduation.mapper.FinishedBatchMapper;
import org.example.graduation.mapper.InspectionRecordMapper;
import org.example.graduation.mapper.InventoryMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.PurchaseOrderMapper;
import org.example.graduation.mapper.SalesOrderMapper;
import org.example.graduation.mapper.ShipmentMapper;
import org.example.graduation.mapper.WorkOrderMapper;
import org.example.graduation.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final MaterialBatchMapper materialBatchMapper;
    private final WorkOrderMapper workOrderMapper;
    private final FinishedBatchMapper finishedBatchMapper;
    private final InventoryMapper inventoryMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final ShipmentMapper shipmentMapper;
    private final AfterSalesRecordMapper afterSalesMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchaseOrderCount", count(purchaseOrderMapper.selectCount(null)));
        result.put("pendingInspectionCount", count(materialBatchMapper.selectCount(
                new LambdaQueryWrapper<MaterialBatch>()
                        .in(MaterialBatch::getInspectionStatus,
                                List.of(ProcurementStatus.PENDING_INSPECTION,
                                        ProcurementStatus.INSPECTING,
                                        ProcurementStatus.PENDING_REVIEW)))));
        result.put("workOrderInProgress", count(workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrder>()
                        .in(WorkOrder::getStatus,
                                List.of(ProductionStatus.WO_IN_PROGRESS,
                                        ProductionStatus.WO_PENDING_QC,
                                        ProductionStatus.WO_REWORKING)))));
        result.put("finishedBatchPendingInbound", count(finishedBatchMapper.selectCount(
                new LambdaQueryWrapper<FinishedBatch>()
                        .eq(FinishedBatch::getStatus, ProductionStatus.BATCH_PENDING_INBOUND))));

        List<Inventory> inventory = inventoryMapper.selectList(null);
        result.put("materialStock", sumStock(inventory, InventoryConstants.ITEM_MATERIAL));
        result.put("finishedStock", sumStock(inventory, InventoryConstants.ITEM_FINISHED));

        result.put("salesOrderCount", count(salesOrderMapper.selectCount(null)));
        result.put("shipmentCount", count(shipmentMapper.selectCount(null)));
        result.put("afterSalesPending", count(afterSalesMapper.selectCount(
                new LambdaQueryWrapper<AfterSalesRecord>()
                        .in(AfterSalesRecord::getStatus,
                                List.of(SalesStatus.AFTER_PENDING,
                                        SalesStatus.AFTER_PROCESSING,
                                        SalesStatus.AFTER_PENDING_ACCEPTANCE)))));
        return result;
    }

    @Override
    public Map<String, Object> quality() {
        List<MaterialBatch> batches = materialBatchMapper.selectList(null);
        Map<String, Long> batchStatus = new LinkedHashMap<>();
        for (MaterialBatch batch : batches) {
            batchStatus.merge(batch.getInspectionStatus(), 1L, Long::sum);
        }

        List<InspectionRecord> records = inspectionRecordMapper.selectList(null);
        long qualified = records.stream()
                .filter(record -> ProcurementStatus.QUALIFIED.equals(record.getConclusion()))
                .count();
        long unqualified = records.stream()
                .filter(record -> ProcurementStatus.UNQUALIFIED.equals(record.getConclusion()))
                .count();
        long total = qualified + unqualified;
        BigDecimal passRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(qualified)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchStatusCount", batchStatus);
        result.put("inspectionTotal", records.size());
        result.put("qualifiedCount", qualified);
        result.put("unqualifiedCount", unqualified);
        result.put("passRate", passRate);
        return result;
    }

    private BigDecimal sumStock(List<Inventory> inventory, String itemType) {
        return inventory.stream()
                .filter(item -> itemType.equals(item.getItemType()))
                .map(Inventory::getAvailableQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }
}