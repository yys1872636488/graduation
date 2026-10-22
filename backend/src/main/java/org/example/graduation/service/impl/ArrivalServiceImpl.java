package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProcurementStatus;
import org.example.graduation.dto.ArrivalCreateDTO;
import org.example.graduation.entity.ArrivalRecord;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.PurchaseOrder;
import org.example.graduation.entity.PurchaseOrderItem;
import org.example.graduation.entity.Supplier;
import org.example.graduation.mapper.ArrivalRecordMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.PurchaseOrderItemMapper;
import org.example.graduation.mapper.PurchaseOrderMapper;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.ArrivalService;
import org.example.graduation.vo.ArrivalVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArrivalServiceImpl implements ArrivalService {

    private final ArrivalRecordMapper arrivalMapper;
    private final MaterialBatchMapper batchMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<ArrivalVO> list(Integer page, Integer size, String keyword, Long orderId) {
        LambdaQueryWrapper<ArrivalRecord> wrapper = new LambdaQueryWrapper<ArrivalRecord>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(ArrivalRecord::getArrivalNo, keyword)
                        .or()
                        .like(ArrivalRecord::getSupplierBatchNo, keyword))
                .eq(orderId != null, ArrivalRecord::getOrderId, orderId)
                .orderByDesc(ArrivalRecord::getCreatedAt);

        Page<ArrivalRecord> arrivalPage = arrivalMapper.selectPage(new Page<>(page, size), wrapper);
        List<ArrivalRecord> records = arrivalPage.getRecords();

        Map<Long, PurchaseOrder> orders = loadOrders(
                records.stream().map(ArrivalRecord::getOrderId).toList());
        Map<Long, Material> materials = loadMaterials(
                records.stream().map(ArrivalRecord::getMaterialId).toList());
        Map<Long, Supplier> suppliers = loadSuppliers(
                records.stream().map(ArrivalRecord::getSupplierId).toList());
        Map<Long, MaterialBatch> batches = loadBatches(
                records.stream().map(ArrivalRecord::getId).toList());

        List<ArrivalVO> vos = records.stream()
                .map(record -> toVO(record, orders.get(record.getOrderId()),
                        materials.get(record.getMaterialId()),
                        suppliers.get(record.getSupplierId()),
                        batches.get(record.getId())))
                .toList();

        Page<ArrivalVO> result = new Page<>(
                arrivalPage.getCurrent(), arrivalPage.getSize(), arrivalPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public ArrivalVO register(Long orderId, ArrivalCreateDTO dto) {
        PurchaseOrder order = orderMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        if (!ProcurementStatus.SUBMITTED.equals(order.getStatus())
                && !ProcurementStatus.PARTIAL_RECEIVED.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不能登记到货");
        }

        // 锁定明细，防止并发到货造成超收
        PurchaseOrderItem item = itemMapper.selectForUpdate(dto.getOrderItemId());
        if (item == null || !orderId.equals(item.getOrderId())) {
            throw new BusinessException("采购明细不属于该订单");
        }

        BigDecimal received = item.getReceivedQuantity() == null
                ? BigDecimal.ZERO : item.getReceivedQuantity();
        BigDecimal remaining = item.getQuantity().subtract(received);
        if (dto.getQuantity().compareTo(remaining) > 0) {
            throw new BusinessException("到货数量不能超过剩余未到货数量 " + remaining.stripTrailingZeros().toPlainString());
        }

        ArrivalRecord arrival = new ArrivalRecord();
        arrival.setArrivalNo(generateArrivalNo());
        arrival.setOrderId(orderId);
        arrival.setOrderItemId(item.getId());
        arrival.setMaterialId(item.getMaterialId());
        arrival.setSupplierId(order.getSupplierId());
        arrival.setQuantity(dto.getQuantity());
        arrival.setUnit(item.getUnit());
        arrival.setArrivalDate(dto.getArrivalDate());
        arrival.setSupplierBatchNo(dto.getSupplierBatchNo() == null ? "" : dto.getSupplierBatchNo());
        arrival.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        arrival.setCreatedBy(currentUserProvider.getUserId());
        arrivalMapper.insert(arrival);

        MaterialBatch batch = new MaterialBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setMaterialId(item.getMaterialId());
        batch.setSupplierId(order.getSupplierId());
        batch.setPurchaseOrderId(orderId);
        batch.setPurchaseOrderItemId(item.getId());
        batch.setArrivalId(arrival.getId());
        batch.setSupplierBatchNo(arrival.getSupplierBatchNo());
        batch.setQuantity(dto.getQuantity());
        batch.setUnit(item.getUnit());
        batch.setArrivalDate(dto.getArrivalDate());
        batch.setInspectionStatus(ProcurementStatus.PENDING_INSPECTION);
        batch.setAvailableQuantity(BigDecimal.ZERO);
        batch.setCreatedBy(currentUserProvider.getUserId());
        batchMapper.insert(batch);

        item.setReceivedQuantity(received.add(dto.getQuantity()));
        itemMapper.updateById(item);

        refreshOrderStatus(order);

        return toVO(arrival, order, materialMapper.selectById(item.getMaterialId()),
                supplierMapper.selectById(order.getSupplierId()), batch);
    }

    private void refreshOrderStatus(PurchaseOrder order) {
        List<PurchaseOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, order.getId()));
        boolean allReceived = !items.isEmpty() && items.stream().allMatch(item -> {
            BigDecimal received = item.getReceivedQuantity() == null
                    ? BigDecimal.ZERO : item.getReceivedQuantity();
            return received.compareTo(item.getQuantity()) >= 0;
        });
        order.setStatus(allReceived
                ? ProcurementStatus.COMPLETED
                : ProcurementStatus.PARTIAL_RECEIVED);
        orderMapper.updateById(order);
    }

    private ArrivalVO toVO(ArrivalRecord arrival, PurchaseOrder order, Material material,
                           Supplier supplier, MaterialBatch batch) {
        return ArrivalVO.builder()
                .id(arrival.getId())
                .arrivalNo(arrival.getArrivalNo())
                .orderId(arrival.getOrderId())
                .orderNo(order == null ? null : order.getOrderNo())
                .orderItemId(arrival.getOrderItemId())
                .materialId(arrival.getMaterialId())
                .materialCode(material == null ? null : material.getMaterialCode())
                .materialName(material == null ? null : material.getName())
                .supplierName(supplier == null ? null : supplier.getName())
                .quantity(arrival.getQuantity())
                .unit(arrival.getUnit())
                .arrivalDate(arrival.getArrivalDate())
                .supplierBatchNo(arrival.getSupplierBatchNo())
                .batchId(batch == null ? null : batch.getId())
                .batchNo(batch == null ? null : batch.getBatchNo())
                .remark(arrival.getRemark())
                .createdBy(arrival.getCreatedBy())
                .createdAt(arrival.getCreatedAt())
                .build();
    }

    private Map<Long, PurchaseOrder> loadOrders(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, order -> order));
    }

    private Map<Long, Material> loadMaterials(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private Map<Long, Supplier> loadSuppliers(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));
    }

    private Map<Long, MaterialBatch> loadBatches(Collection<Long> arrivalIds) {
        List<Long> distinct = arrivalIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchMapper.selectList(new LambdaQueryWrapper<MaterialBatch>()
                        .in(MaterialBatch::getArrivalId, distinct))
                .stream()
                .collect(Collectors.toMap(MaterialBatch::getArrivalId, batch -> batch, (a, b) -> a));
    }

    private String generateArrivalNo() {
        String prefix = "ARR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = arrivalMapper.selectCount(new LambdaQueryWrapper<ArrivalRecord>()
                .likeRight(ArrivalRecord::getArrivalNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateBatchNo() {
        String prefix = "MB-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = batchMapper.selectCount(new LambdaQueryWrapper<MaterialBatch>()
                .likeRight(MaterialBatch::getBatchNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}