package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.AfterSalesRecord;
import org.example.graduation.entity.ArrivalRecord;
import org.example.graduation.entity.FinishedBatch;
import org.example.graduation.entity.FinishedBatchMaterial;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.MaterialIssue;
import org.example.graduation.entity.MaterialIssueItem;
import org.example.graduation.entity.PurchaseOrder;
import org.example.graduation.entity.SalesOrder;
import org.example.graduation.entity.Shipment;
import org.example.graduation.entity.ShipmentItem;
import org.example.graduation.entity.Supplier;
import org.example.graduation.mapper.AfterSalesRecordMapper;
import org.example.graduation.mapper.ArrivalRecordMapper;
import org.example.graduation.mapper.FinishedBatchMapper;
import org.example.graduation.mapper.FinishedBatchMaterialMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialIssueItemMapper;
import org.example.graduation.mapper.MaterialIssueMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.PurchaseOrderMapper;
import org.example.graduation.mapper.SalesOrderMapper;
import org.example.graduation.mapper.ShipmentItemMapper;
import org.example.graduation.mapper.ShipmentMapper;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.service.InspectionService;
import org.example.graduation.service.MaterialBatchService;
import org.example.graduation.service.TraceabilityService;
import org.example.graduation.service.WorkOrderService;
import org.example.graduation.vo.FinishedBatchVO;
import org.example.graduation.vo.WorkOrderVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceabilityServiceImpl implements TraceabilityService {

    private final MaterialBatchMapper materialBatchMapper;
    private final FinishedBatchMapper finishedBatchMapper;
    private final FinishedBatchMaterialMapper finishedBatchMaterialMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ArrivalRecordMapper arrivalMapper;
    private final MaterialIssueMapper issueMapper;
    private final MaterialIssueItemMapper issueItemMapper;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentItemMapper shipmentItemMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final AfterSalesRecordMapper afterSalesMapper;
    private final MaterialBatchService materialBatchService;
    private final InspectionService inspectionService;
    private final WorkOrderService workOrderService;

    @Override
    public Map<String, Object> traceMaterialBatch(String batchNo) {
        MaterialBatch batch = materialBatchMapper.selectOne(
                new LambdaQueryWrapper<MaterialBatch>()
                        .eq(MaterialBatch::getBatchNo, batchNo));
        if (batch == null) {
            throw new BusinessException("原材料批次不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", materialBatchService.detail(batch.getId()));
        result.put("material", materialMapper.selectById(batch.getMaterialId()));
        result.put("supplier", supplierMapper.selectById(batch.getSupplierId()));
        result.put("purchaseOrder", purchaseOrderMapper.selectById(batch.getPurchaseOrderId()));
        result.put("arrival", arrivalMapper.selectById(batch.getArrivalId()));
        result.put("inspection", inspectionService.detail(batch.getId()));

        List<MaterialIssueItem> issueItems = issueItemMapper.selectList(
                new LambdaQueryWrapper<MaterialIssueItem>()
                        .eq(MaterialIssueItem::getMaterialBatchId, batch.getId()));
        List<Long> issueIds = issueItems.stream().map(MaterialIssueItem::getIssueId)
                .distinct().toList();
        List<MaterialIssue> issues = issueIds.isEmpty()
                ? List.of() : issueMapper.selectBatchIds(issueIds);
        result.put("materialIssues", issues);

        Set<Long> workOrderIds = issues.stream().map(MaterialIssue::getWorkOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<WorkOrderVO> workOrders = workOrderIds.stream()
                .map(workOrderService::detail).toList();
        result.put("workOrders", workOrders);

        List<FinishedBatchVO> finishedBatches = workOrders.stream()
                .flatMap(order -> order.getBatches() == null
                        ? java.util.stream.Stream.empty() : order.getBatches().stream())
                .toList();
        result.put("finishedBatches", finishedBatches);

        List<Long> finishedBatchIds = finishedBatches.stream()
                .map(FinishedBatchVO::getId).toList();
        Map<String, Object> shipmentData = loadShipmentData(finishedBatchIds);
        result.putAll(shipmentData);
        List<SalesOrder> salesOrders = castList(shipmentData.get("salesOrders"));
        List<Long> salesOrderIds = salesOrders.stream().map(SalesOrder::getId).toList();
        result.put("afterSales", loadAfterSales(finishedBatchIds, salesOrderIds));
        return result;
    }

    @Override
    public Map<String, Object> traceFinishedBatch(String batchNo) {
        FinishedBatch batch = finishedBatchMapper.selectOne(
                new LambdaQueryWrapper<FinishedBatch>().eq(FinishedBatch::getBatchNo, batchNo));
        if (batch == null) {
            throw new BusinessException("成品批次不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("finishedBatch", batch);
        result.put("workOrder", workOrderService.detail(batch.getWorkOrderId()));

        List<FinishedBatchMaterial> relations = finishedBatchMaterialMapper.selectList(
                new LambdaQueryWrapper<FinishedBatchMaterial>()
                        .eq(FinishedBatchMaterial::getFinishedBatchId, batch.getId()));
        List<Map<String, Object>> upstream = new ArrayList<>();
        for (FinishedBatchMaterial relation : relations) {
            MaterialBatch materialBatch = materialBatchMapper.selectById(
                    relation.getMaterialBatchId());
            if (materialBatch == null) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("usedQuantity", relation.getQuantity());
            node.put("batch", materialBatchService.detail(materialBatch.getId()));
            node.put("supplier", supplierMapper.selectById(materialBatch.getSupplierId()));
            node.put("purchaseOrder",
                    purchaseOrderMapper.selectById(materialBatch.getPurchaseOrderId()));
            node.put("inspection", inspectionService.detail(materialBatch.getId()));
            upstream.add(node);
        }
        result.put("materialBatches", upstream);

        Map<String, Object> shipmentData = loadShipmentData(List.of(batch.getId()));
        result.putAll(shipmentData);
        List<SalesOrder> salesOrders = castList(shipmentData.get("salesOrders"));
        List<Long> salesOrderIds = salesOrders.stream().map(SalesOrder::getId).toList();
        result.put("afterSales", loadAfterSales(List.of(batch.getId()), salesOrderIds));
        return result;
    }

    private Map<String, Object> loadShipmentData(List<Long> finishedBatchIds) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (finishedBatchIds == null || finishedBatchIds.isEmpty()) {
            data.put("shipmentItems", List.of());
            data.put("shipments", List.of());
            data.put("salesOrders", List.of());
            return data;
        }
        List<ShipmentItem> shipmentItems = shipmentItemMapper.selectList(
                new LambdaQueryWrapper<ShipmentItem>()
                        .in(ShipmentItem::getFinishedBatchId, finishedBatchIds));
        data.put("shipmentItems", shipmentItems);

        List<Long> shipmentIds = shipmentItems.stream().map(ShipmentItem::getShipmentId)
                .distinct().toList();
        List<Shipment> shipments = shipmentIds.isEmpty()
                ? List.of() : shipmentMapper.selectBatchIds(shipmentIds);
        data.put("shipments", shipments);

        List<Long> orderIds = shipments.stream().map(Shipment::getOrderId)
                .distinct().toList();
        List<SalesOrder> salesOrders = orderIds.isEmpty()
                ? List.of() : salesOrderMapper.selectBatchIds(orderIds);
        data.put("salesOrders", salesOrders);
        return data;
    }

    private List<AfterSalesRecord> loadAfterSales(List<Long> finishedBatchIds,
                                                  List<Long> salesOrderIds) {
        boolean hasBatch = finishedBatchIds != null && !finishedBatchIds.isEmpty();
        boolean hasOrder = salesOrderIds != null && !salesOrderIds.isEmpty();
        if (!hasBatch && !hasOrder) {
            return List.of();
        }
        LambdaQueryWrapper<AfterSalesRecord> wrapper = new LambdaQueryWrapper<>();
        if (hasBatch && hasOrder) {
            wrapper.and(q -> q.in(AfterSalesRecord::getFinishedBatchId, finishedBatchIds)
                    .or()
                    .in(AfterSalesRecord::getSalesOrderId, salesOrderIds));
        } else if (hasBatch) {
            wrapper.in(AfterSalesRecord::getFinishedBatchId, finishedBatchIds);
        } else {
            wrapper.in(AfterSalesRecord::getSalesOrderId, salesOrderIds);
        }
        wrapper.orderByDesc(AfterSalesRecord::getCreatedAt);
        return afterSalesMapper.selectList(wrapper);
    }

    @SuppressWarnings("unchecked")
    private List<SalesOrder> castList(Object value) {
        if (value instanceof List<?> list) {
            return (List<SalesOrder>) list;
        }
        return Collections.emptyList();
    }
}