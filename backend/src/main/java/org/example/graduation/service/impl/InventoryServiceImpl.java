package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.InventoryConstants;
import org.example.graduation.common.ProcurementStatus;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.entity.FinishedBatch;
import org.example.graduation.entity.Inventory;
import org.example.graduation.entity.InventoryTransaction;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.MaterialBatch;
import org.example.graduation.entity.MaterialIssueItem;
import org.example.graduation.entity.Product;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.entity.Warehouse;
import org.example.graduation.mapper.FinishedBatchMapper;
import org.example.graduation.mapper.InventoryMapper;
import org.example.graduation.mapper.InventoryTransactionMapper;
import org.example.graduation.mapper.MaterialBatchMapper;
import org.example.graduation.mapper.MaterialIssueItemMapper;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.mapper.ProductSkuMapper;
import org.example.graduation.mapper.WarehouseMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.InventoryService;
import org.example.graduation.vo.InventoryTransactionVO;
import org.example.graduation.vo.InventoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final MaterialBatchMapper materialBatchMapper;
    private final FinishedBatchMapper finishedBatchMapper;
    private final MaterialMapper materialMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final MaterialIssueItemMapper issueItemMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<InventoryVO> list(Integer page, Integer size, Long warehouseId,
                                  String itemType, String keyword) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(itemType), Inventory::getItemType, itemType)
                .orderByDesc(Inventory::getUpdatedAt);
        if (StringUtils.hasText(keyword)) {
            List<Long> materialBatchIds = materialBatchMapper.selectList(
                            new LambdaQueryWrapper<MaterialBatch>()
                                    .like(MaterialBatch::getBatchNo, keyword))
                    .stream().map(MaterialBatch::getId).toList();
            List<Long> finishedBatchIds = finishedBatchMapper.selectList(
                            new LambdaQueryWrapper<FinishedBatch>()
                                    .like(FinishedBatch::getBatchNo, keyword))
                    .stream().map(FinishedBatch::getId).toList();
            List<Long> allIds = new java.util.ArrayList<>();
            allIds.addAll(materialBatchIds);
            allIds.addAll(finishedBatchIds);
            if (allIds.isEmpty()) {
                return emptyPage(page, size);
            }
            wrapper.in(Inventory::getBatchId, allIds);
        }

        Page<Inventory> inventoryPage = inventoryMapper.selectPage(
                new Page<>(page, size), wrapper);
        List<Inventory> records = inventoryPage.getRecords();
        Map<Long, Warehouse> warehouses = loadWarehouses(
                records.stream().map(Inventory::getWarehouseId).toList());
        Map<Long, MaterialBatch> materialBatches = loadMaterialBatches(
                records.stream()
                        .filter(item -> InventoryConstants.ITEM_MATERIAL.equals(item.getItemType()))
                        .map(Inventory::getBatchId).toList());
        Map<Long, FinishedBatch> finishedBatches = loadFinishedBatches(
                records.stream()
                        .filter(item -> InventoryConstants.ITEM_FINISHED.equals(item.getItemType()))
                        .map(Inventory::getBatchId).toList());
        Map<Long, Material> materials = loadMaterials(
                materialBatches.values().stream().map(MaterialBatch::getMaterialId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                finishedBatches.values().stream().map(FinishedBatch::getSkuId).toList());
        Map<Long, Product> products = loadProducts(
                skus.values().stream().map(ProductSku::getProductId).toList());

        List<InventoryVO> vos = records.stream()
                .map(item -> {
                    boolean materialType = InventoryConstants.ITEM_MATERIAL.equals(item.getItemType());
                    return toVO(item, warehouses.get(item.getWarehouseId()),
                            materialType ? materialBatches.get(item.getBatchId()) : null,
                            materialType ? null : finishedBatches.get(item.getBatchId()),
                            materials.get(item.getMaterialId()),
                            skus.get(item.getSkuId()),
                            products.get(item.getSkuId()));
                })
                .toList();
        Page<InventoryVO> result = new Page<>(
                inventoryPage.getCurrent(), inventoryPage.getSize(), inventoryPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public Page<InventoryTransactionVO> listTransactions(Integer page, Integer size,
                                                         Long warehouseId, String businessType,
                                                         String keyword) {
        Page<InventoryTransaction> transactionPage = transactionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<InventoryTransaction>()
                        .eq(warehouseId != null, InventoryTransaction::getWarehouseId, warehouseId)
                        .eq(StringUtils.hasText(businessType),
                                InventoryTransaction::getBusinessType, businessType)
                        .and(StringUtils.hasText(keyword), q -> q
                                .like(InventoryTransaction::getTransactionNo, keyword)
                                .or()
                                .like(InventoryTransaction::getBusinessNo, keyword))
                        .orderByDesc(InventoryTransaction::getCreatedAt));
        List<InventoryTransaction> records = transactionPage.getRecords();
        Map<Long, Warehouse> warehouses = loadWarehouses(
                records.stream().map(InventoryTransaction::getWarehouseId).toList());
        Map<Long, MaterialBatch> materialBatches = loadMaterialBatches(
                records.stream()
                        .filter(item -> InventoryConstants.ITEM_MATERIAL.equals(item.getItemType()))
                        .map(InventoryTransaction::getBatchId).toList());
        Map<Long, FinishedBatch> finishedBatches = loadFinishedBatches(
                records.stream()
                        .filter(item -> InventoryConstants.ITEM_FINISHED.equals(item.getItemType()))
                        .map(InventoryTransaction::getBatchId).toList());
        Map<Long, Material> materials = loadMaterials(
                records.stream().map(InventoryTransaction::getMaterialId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                records.stream().map(InventoryTransaction::getSkuId).toList());

        List<InventoryTransactionVO> vos = records.stream().map(item -> {
            boolean materialType = InventoryConstants.ITEM_MATERIAL.equals(item.getItemType());
            MaterialBatch materialBatch = materialType
                    ? materialBatches.get(item.getBatchId()) : null;
            FinishedBatch finishedBatch = materialType
                    ? null : finishedBatches.get(item.getBatchId());
            String batchNo = materialBatch != null ? materialBatch.getBatchNo()
                    : (finishedBatch == null ? null : finishedBatch.getBatchNo());
            return InventoryTransactionVO.builder()
                    .id(item.getId())
                    .transactionNo(item.getTransactionNo())
                    .warehouseId(item.getWarehouseId())
                    .warehouseName(warehouses.get(item.getWarehouseId()) == null
                            ? null : warehouses.get(item.getWarehouseId()).getName())
                    .itemType(item.getItemType())
                    .materialId(item.getMaterialId())
                    .materialName(materials.get(item.getMaterialId()) == null
                            ? null : materials.get(item.getMaterialId()).getName())
                    .skuId(item.getSkuId())
                    .skuCode(skus.get(item.getSkuId()) == null
                            ? null : skus.get(item.getSkuId()).getSkuCode())
                    .batchId(item.getBatchId())
                    .batchNo(batchNo)
                    .businessType(item.getBusinessType())
                    .direction(item.getDirection())
                    .quantity(item.getQuantity())
                    .beforeQuantity(item.getBeforeQuantity())
                    .afterQuantity(item.getAfterQuantity())
                    .businessNo(item.getBusinessNo())
                    .operatorId(item.getOperatorId())
                    .remark(item.getRemark())
                    .createdAt(item.getCreatedAt())
                    .build();
        }).toList();
        Page<InventoryTransactionVO> result = new Page<>(
                transactionPage.getCurrent(), transactionPage.getSize(), transactionPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public InventoryVO materialInbound(Long materialBatchId, Long warehouseId) {
        MaterialBatch batch = materialBatchMapper.selectForUpdate(materialBatchId);
        if (batch == null) {
            throw new BusinessException("原材料批次不存在");
        }
        if (!ProcurementStatus.QUALIFIED.equals(batch.getInspectionStatus())) {
            throw new BusinessException("只有质检合格的批次可以入库");
        }
        Warehouse warehouse = requireWarehouse(warehouseId);
        if (findInventory(InventoryConstants.ITEM_MATERIAL, batch.getId()) != null) {
            throw new BusinessException("该原材料批次已入库");
        }

        BigDecimal issued = sumIssued(batch.getId());
        BigDecimal quantity = batch.getQuantity().subtract(issued);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("该批次已无可入库数量");
        }

        Inventory inventory = new Inventory();
        inventory.setWarehouseId(warehouseId);
        inventory.setItemType(InventoryConstants.ITEM_MATERIAL);
        inventory.setMaterialId(batch.getMaterialId());
        inventory.setBatchId(batch.getId());
        inventory.setQuantity(quantity);
        inventory.setAvailableQuantity(quantity);
        inventoryMapper.insert(inventory);

        batch.setInspectionStatus(ProcurementStatus.STOCKED);
        batch.setAvailableQuantity(quantity);
        materialBatchMapper.updateById(batch);

        recordTransaction(warehouseId, InventoryConstants.ITEM_MATERIAL,
                batch.getMaterialId(), null, batch.getId(),
                InventoryConstants.MATERIAL_INBOUND, InventoryConstants.DIRECTION_IN,
                quantity, BigDecimal.ZERO, quantity, null, "原材料质检合格入库");

        return toVO(inventory, warehouse, batch, null,
                materialMapper.selectById(batch.getMaterialId()), null, null);
    }

    @Override
    @Transactional
    public InventoryVO finishedInbound(Long finishedBatchId, Long warehouseId) {
        FinishedBatch batch = finishedBatchMapper.selectForUpdate(finishedBatchId);
        if (batch == null) {
            throw new BusinessException("成品批次不存在");
        }
        if (!ProductionStatus.BATCH_PENDING_INBOUND.equals(batch.getStatus())) {
            throw new BusinessException("只有待入库的成品批次可以入库");
        }
        Warehouse warehouse = requireWarehouse(warehouseId);
        if (findInventory(InventoryConstants.ITEM_FINISHED, batch.getId()) != null) {
            throw new BusinessException("该成品批次已入库");
        }

        Inventory inventory = new Inventory();
        inventory.setWarehouseId(warehouseId);
        inventory.setItemType(InventoryConstants.ITEM_FINISHED);
        inventory.setSkuId(batch.getSkuId());
        inventory.setBatchId(batch.getId());
        inventory.setQuantity(batch.getQuantity());
        inventory.setAvailableQuantity(batch.getQuantity());
        inventoryMapper.insert(inventory);

        batch.setStatus(ProductionStatus.BATCH_AVAILABLE);
        batch.setWarehouseId(warehouseId);
        finishedBatchMapper.updateById(batch);

        recordTransaction(warehouseId, InventoryConstants.ITEM_FINISHED,
                null, batch.getSkuId(), batch.getId(),
                InventoryConstants.FINISHED_INBOUND, InventoryConstants.DIRECTION_IN,
                batch.getQuantity(), BigDecimal.ZERO, batch.getQuantity(),
                batch.getWorkOrderId() == null ? null : "WO#" + batch.getWorkOrderId(),
                "成品批次入库");

        ProductSku sku = skuMapper.selectById(batch.getSkuId());
        return toVO(inventory, warehouse, null, batch, null, sku,
                sku == null ? null : productMapper.selectById(sku.getProductId()));
    }

    @Override
    @Transactional
    public void issueMaterial(Long materialBatchId, BigDecimal quantity,
                              String businessNo, String remark) {
        Inventory inventory = inventoryMapper.selectMaterialForUpdate(materialBatchId, quantity);
        if (inventory == null) {
            throw new BusinessException("原材料库存不足，请先完成合格入库");
        }
        BigDecimal before = inventory.getAvailableQuantity();
        BigDecimal after = before.subtract(quantity);
        inventory.setQuantity(inventory.getQuantity().subtract(quantity));
        inventory.setAvailableQuantity(after);
        inventoryMapper.updateById(inventory);

        MaterialBatch batch = materialBatchMapper.selectForUpdate(materialBatchId);
        if (batch != null) {
            batch.setAvailableQuantity(after);
            if (after.compareTo(BigDecimal.ZERO) == 0) {
                batch.setInspectionStatus(ProcurementStatus.DEPLETED);
            }
            materialBatchMapper.updateById(batch);
        }

        recordTransaction(inventory.getWarehouseId(), InventoryConstants.ITEM_MATERIAL,
                inventory.getMaterialId(), null, materialBatchId,
                InventoryConstants.MATERIAL_ISSUE, InventoryConstants.DIRECTION_OUT,
                quantity, before, after, businessNo, remark);
    }

    @Override
    @Transactional
    public void salesOutbound(Long finishedBatchId, BigDecimal quantity,
                              String businessNo, String remark) {
        Inventory inventory = inventoryMapper.selectFinishedForUpdate(finishedBatchId, quantity);
        if (inventory == null) {
            throw new BusinessException("成品库存不足，无法发货");
        }
        BigDecimal before = inventory.getAvailableQuantity();
        BigDecimal after = before.subtract(quantity);
        inventory.setQuantity(inventory.getQuantity().subtract(quantity));
        inventory.setAvailableQuantity(after);
        inventoryMapper.updateById(inventory);

        FinishedBatch batch = finishedBatchMapper.selectForUpdate(finishedBatchId);
        if (batch != null && after.compareTo(BigDecimal.ZERO) == 0) {
            batch.setStatus(ProductionStatus.BATCH_DEPLETED);
            finishedBatchMapper.updateById(batch);
        }

        recordTransaction(inventory.getWarehouseId(), InventoryConstants.ITEM_FINISHED,
                null, inventory.getSkuId(), finishedBatchId,
                InventoryConstants.SALES_OUTBOUND, InventoryConstants.DIRECTION_OUT,
                quantity, before, after, businessNo, remark);
    }

    private void recordTransaction(Long warehouseId, String itemType, Long materialId,
                                   Long skuId, Long batchId, String businessType,
                                   String direction, BigDecimal quantity,
                                   BigDecimal before, BigDecimal after,
                                   String businessNo, String remark) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionNo(generateTransactionNo());
        transaction.setWarehouseId(warehouseId);
        transaction.setItemType(itemType);
        transaction.setMaterialId(materialId);
        transaction.setSkuId(skuId);
        transaction.setBatchId(batchId);
        transaction.setBusinessType(businessType);
        transaction.setDirection(direction);
        transaction.setQuantity(quantity);
        transaction.setBeforeQuantity(before);
        transaction.setAfterQuantity(after);
        transaction.setBusinessNo(businessNo == null ? "" : businessNo);
        transaction.setOperatorId(currentUserProvider.getUserId());
        transaction.setRemark(remark == null ? "" : remark);
        transactionMapper.insert(transaction);
    }

    private Inventory findInventory(String itemType, Long batchId) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getItemType, itemType)
                .eq(Inventory::getBatchId, batchId)
                .last("LIMIT 1"));
    }

    private Warehouse requireWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || warehouse.getStatus() == null || warehouse.getStatus() != 1) {
            throw new BusinessException("仓库不存在或已停用");
        }
        return warehouse;
    }

    private BigDecimal sumIssued(Long materialBatchId) {
        return issueItemMapper.selectList(new LambdaQueryWrapper<MaterialIssueItem>()
                        .eq(MaterialIssueItem::getMaterialBatchId, materialBatchId))
                .stream().map(MaterialIssueItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InventoryVO toVO(Inventory inventory, Warehouse warehouse,
                             MaterialBatch materialBatch, FinishedBatch finishedBatch,
                             Material material, ProductSku sku, Product product) {
        String unit = materialBatch != null ? materialBatch.getUnit() : "件";
        return InventoryVO.builder()
                .id(inventory.getId())
                .warehouseId(inventory.getWarehouseId())
                .warehouseName(warehouse == null ? null : warehouse.getName())
                .itemType(inventory.getItemType())
                .materialId(inventory.getMaterialId())
                .materialCode(material == null ? null : material.getMaterialCode())
                .materialName(material == null ? null : material.getName())
                .skuId(inventory.getSkuId())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .productName(product == null ? null : product.getName())
                .batchId(inventory.getBatchId())
                .batchNo(materialBatch != null ? materialBatch.getBatchNo()
                        : (finishedBatch == null ? null : finishedBatch.getBatchNo()))
                .quantity(inventory.getQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .unit(unit)
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private Page<InventoryVO> emptyPage(Integer page, Integer size) {
        Page<InventoryVO> empty = new Page<>(page, size, 0);
        empty.setRecords(Collections.emptyList());
        return empty;
    }

    private Map<Long, Warehouse> loadWarehouses(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Warehouse::getId, warehouse -> warehouse));
    }

    private Map<Long, MaterialBatch> loadMaterialBatches(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialBatchMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MaterialBatch::getId, batch -> batch));
    }

    private Map<Long, FinishedBatch> loadFinishedBatches(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return finishedBatchMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(FinishedBatch::getId, batch -> batch));
    }

    private Map<Long, Material> loadMaterials(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private Map<Long, ProductSku> loadSkus(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return skuMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(ProductSku::getId, sku -> sku));
    }

    private Map<Long, Product> loadProducts(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private String generateTransactionNo() {
        String prefix = "IT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = transactionMapper.selectCount(new LambdaQueryWrapper<InventoryTransaction>()
                .likeRight(InventoryTransaction::getTransactionNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}