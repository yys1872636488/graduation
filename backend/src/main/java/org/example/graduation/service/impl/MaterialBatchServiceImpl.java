package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.ArrivalRecord;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.PurchaseOrder;
import org.example.graduation.entity.Supplier;
import org.example.graduation.mapper.ArrivalRecordMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.PurchaseOrderMapper;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.service.MaterialBatchService;
import org.example.graduation.vo.MaterialBatchVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialBatchServiceImpl implements MaterialBatchService {

    private final MaterialBatchMapper batchMapper;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper orderMapper;
    private final ArrivalRecordMapper arrivalMapper;

    @Override
    public Page<MaterialBatchVO> list(Integer page, Integer size, String keyword,
                                      String inspectionStatus, Long supplierId) {
        LambdaQueryWrapper<MaterialBatch> wrapper = new LambdaQueryWrapper<MaterialBatch>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(MaterialBatch::getBatchNo, keyword)
                        .or()
                        .like(MaterialBatch::getSupplierBatchNo, keyword))
                .eq(StringUtils.hasText(inspectionStatus), MaterialBatch::getInspectionStatus, inspectionStatus)
                .eq(supplierId != null, MaterialBatch::getSupplierId, supplierId)
                .orderByDesc(MaterialBatch::getCreatedAt);

        Page<MaterialBatch> batchPage = batchMapper.selectPage(new Page<>(page, size), wrapper);
        List<MaterialBatch> records = batchPage.getRecords();

        Map<Long, Material> materials = loadMaterials(
                records.stream().map(MaterialBatch::getMaterialId).toList());
        Map<Long, Supplier> suppliers = loadSuppliers(
                records.stream().map(MaterialBatch::getSupplierId).toList());
        Map<Long, PurchaseOrder> orders = loadOrders(
                records.stream().map(MaterialBatch::getPurchaseOrderId).toList());
        Map<Long, ArrivalRecord> arrivals = loadArrivals(
                records.stream().map(MaterialBatch::getArrivalId).toList());

        List<MaterialBatchVO> vos = records.stream()
                .map(batch -> toVO(batch,
                        materials.get(batch.getMaterialId()),
                        suppliers.get(batch.getSupplierId()),
                        orders.get(batch.getPurchaseOrderId()),
                        arrivals.get(batch.getArrivalId())))
                .toList();

        Page<MaterialBatchVO> result = new Page<>(
                batchPage.getCurrent(), batchPage.getSize(), batchPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public MaterialBatchVO detail(Long id) {
        MaterialBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException("原材料批次不存在");
        }
        return toVO(batch,
                materialMapper.selectById(batch.getMaterialId()),
                supplierMapper.selectById(batch.getSupplierId()),
                orderMapper.selectById(batch.getPurchaseOrderId()),
                arrivalMapper.selectById(batch.getArrivalId()));
    }

    private MaterialBatchVO toVO(MaterialBatch batch, Material material, Supplier supplier,
                                 PurchaseOrder order, ArrivalRecord arrival) {
        return MaterialBatchVO.builder()
                .id(batch.getId())
                .batchNo(batch.getBatchNo())
                .materialId(batch.getMaterialId())
                .materialCode(material == null ? null : material.getMaterialCode())
                .materialName(material == null ? null : material.getName())
                .specification(material == null ? null : material.getSpecification())
                .supplierId(batch.getSupplierId())
                .supplierName(supplier == null ? null : supplier.getName())
                .purchaseOrderId(batch.getPurchaseOrderId())
                .orderNo(order == null ? null : order.getOrderNo())
                .arrivalId(batch.getArrivalId())
                .arrivalNo(arrival == null ? null : arrival.getArrivalNo())
                .supplierBatchNo(batch.getSupplierBatchNo())
                .quantity(batch.getQuantity())
                .unit(batch.getUnit())
                .arrivalDate(batch.getArrivalDate())
                .inspectionStatus(batch.getInspectionStatus())
                .availableQuantity(batch.getAvailableQuantity())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    private Map<Long, Material> loadMaterials(Collection<Long> ids) {
        List<Long> distinct = distinct(ids);
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private Map<Long, Supplier> loadSuppliers(Collection<Long> ids) {
        List<Long> distinct = distinct(ids);
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));
    }

    private Map<Long, PurchaseOrder> loadOrders(Collection<Long> ids) {
        List<Long> distinct = distinct(ids);
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, order -> order));
    }

    private Map<Long, ArrivalRecord> loadArrivals(Collection<Long> ids) {
        List<Long> distinct = distinct(ids);
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return arrivalMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(ArrivalRecord::getId, arrival -> arrival));
    }

    private List<Long> distinct(Collection<Long> ids) {
        return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
}