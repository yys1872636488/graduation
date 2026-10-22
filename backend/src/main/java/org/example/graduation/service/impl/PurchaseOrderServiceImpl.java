package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProcurementStatus;
import org.example.graduation.dto.PurchaseOrderItemDTO;
import org.example.graduation.dto.PurchaseOrderSaveDTO;
import org.example.graduation.entity.Material;
import org.example.graduation.entity.PurchaseOrder;
import org.example.graduation.entity.PurchaseOrderItem;
import org.example.graduation.entity.Supplier;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.mapper.PurchaseOrderItemMapper;
import org.example.graduation.mapper.PurchaseOrderMapper;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.PurchaseOrderService;
import org.example.graduation.vo.PurchaseOrderItemVO;
import org.example.graduation.vo.PurchaseOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<PurchaseOrderVO> list(Integer page, Integer size, String keyword,
                                      Long supplierId, String status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<PurchaseOrder>()
                .like(StringUtils.hasText(keyword), PurchaseOrder::getOrderNo, keyword)
                .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(StringUtils.hasText(status), PurchaseOrder::getStatus, status)
                .orderByDesc(PurchaseOrder::getCreatedAt);

        Page<PurchaseOrder> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<Long> orderIds = orderPage.getRecords().stream().map(PurchaseOrder::getId).toList();
        Map<Long, Supplier> suppliers = loadSuppliers(
                orderPage.getRecords().stream().map(PurchaseOrder::getSupplierId).toList());
        Map<Long, List<PurchaseOrderItem>> itemsByOrder = loadItems(orderIds);

        List<PurchaseOrderVO> records = orderPage.getRecords().stream()
                .map(order -> toVO(order, suppliers.get(order.getSupplierId()),
                        itemsByOrder.getOrDefault(order.getId(), List.of()), null, false))
                .toList();

        Page<PurchaseOrderVO> result = new Page<>(
                orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public PurchaseOrderVO detail(Long id) {
        PurchaseOrder order = requireOrder(id);
        List<PurchaseOrderItem> items = loadItems(List.of(id)).getOrDefault(id, List.of());
        Map<Long, Material> materials = loadMaterials(
                items.stream().map(PurchaseOrderItem::getMaterialId).toList());
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        return toVO(order, supplier, items, materials, true);
    }

    @Override
    @Transactional
    public PurchaseOrderVO create(PurchaseOrderSaveDTO dto) {
        validateSupplier(dto.getSupplierId());
        validateItems(dto.getItems());
        validateDates(dto);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setSupplierId(dto.getSupplierId());
        order.setOrderDate(dto.getOrderDate());
        order.setExpectedDate(dto.getExpectedDate());
        order.setStatus(ProcurementStatus.DRAFT);
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        order.setCreatedBy(currentUserProvider.getUserId());
        orderMapper.insert(order);

        insertItems(order.getId(), dto.getItems());
        return detail(order.getId());
    }

    @Override
    @Transactional
    public PurchaseOrderVO update(Long id, PurchaseOrderSaveDTO dto) {
        PurchaseOrder order = requireOrder(id);
        if (!ProcurementStatus.DRAFT.equals(order.getStatus())) {
            throw new BusinessException("只有草稿状态的采购订单可以编辑");
        }
        validateSupplier(dto.getSupplierId());
        validateItems(dto.getItems());
        validateDates(dto);

        order.setSupplierId(dto.getSupplierId());
        order.setOrderDate(dto.getOrderDate());
        order.setExpectedDate(dto.getExpectedDate());
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        orderMapper.updateById(order);

        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));
        insertItems(id, dto.getItems());
        return detail(id);
    }

    @Override
    public void submit(Long id) {
        PurchaseOrder order = requireOrder(id);
        if (!ProcurementStatus.DRAFT.equals(order.getStatus())) {
            throw new BusinessException("只有草稿状态的采购订单可以提交");
        }
        Long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, id));
        if (itemCount == null || itemCount == 0) {
            throw new BusinessException("采购订单没有明细，不能提交");
        }
        order.setStatus(ProcurementStatus.SUBMITTED);
        order.setSubmittedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public void cancel(Long id) {
        PurchaseOrder order = requireOrder(id);
        if (!ProcurementStatus.DRAFT.equals(order.getStatus())
                && !ProcurementStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BusinessException("当前状态的采购订单不能取消");
        }
        List<PurchaseOrderItem> items = loadItems(List.of(id)).getOrDefault(id, List.of());
        boolean received = items.stream().anyMatch(item ->
                item.getReceivedQuantity() != null
                        && item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);
        if (received) {
            throw new BusinessException("采购订单已有到货记录，不能取消");
        }
        order.setStatus(ProcurementStatus.CANCELLED);
        orderMapper.updateById(order);
    }

    private PurchaseOrder requireOrder(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("采购订单不存在");
        }
        return order;
    }

    private void validateSupplier(Long supplierId) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || supplier.getStatus() == null || supplier.getStatus() != 1) {
            throw new BusinessException("供应商不存在或已停用");
        }
    }

    private void validateItems(List<PurchaseOrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("请至少添加一条采购明细");
        }
        for (PurchaseOrderItemDTO item : items) {
            Material material = materialMapper.selectById(item.getMaterialId());
            if (material == null || material.getStatus() == null || material.getStatus() != 1) {
                throw new BusinessException("原材料不存在或已停用");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("采购数量必须大于0");
            }
            if (!StringUtils.hasText(item.getUnit())) {
                item.setUnit(material.getUnit());
            }
        }
    }

    private void validateDates(PurchaseOrderSaveDTO dto) {
        if (dto.getExpectedDate() != null && dto.getExpectedDate().isBefore(dto.getOrderDate())) {
            throw new BusinessException("预计到货日期不能早于下单日期");
        }
    }

    private void insertItems(Long orderId, List<PurchaseOrderItemDTO> items) {
        for (PurchaseOrderItemDTO dto : items) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(orderId);
            item.setMaterialId(dto.getMaterialId());
            item.setQuantity(dto.getQuantity());
            item.setUnit(dto.getUnit());
            item.setUnitPrice(dto.getUnitPrice());
            item.setReceivedQuantity(BigDecimal.ZERO);
            item.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
            itemMapper.insert(item);
        }
    }

    private PurchaseOrderVO toVO(PurchaseOrder order, Supplier supplier,
                                 List<PurchaseOrderItem> items,
                                 Map<Long, Material> materials, boolean withItems) {
        BigDecimal totalAmount = items.stream()
                .map(item -> item.getUnitPrice() == null
                        ? BigDecimal.ZERO
                        : item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PurchaseOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierId(order.getSupplierId())
                .supplierName(supplier == null ? null : supplier.getName())
                .orderDate(order.getOrderDate())
                .expectedDate(order.getExpectedDate())
                .status(order.getStatus())
                .remark(order.getRemark())
                .createdBy(order.getCreatedBy())
                .submittedAt(order.getSubmittedAt())
                .createdAt(order.getCreatedAt())
                .itemCount(items.size())
                .totalAmount(totalAmount)
                .items(withItems ? items.stream()
                        .map(item -> toItemVO(item, materials == null ? null : materials.get(item.getMaterialId())))
                        .toList() : null)
                .build();
    }

    private PurchaseOrderItemVO toItemVO(PurchaseOrderItem item, Material material) {
        BigDecimal received = item.getReceivedQuantity() == null
                ? BigDecimal.ZERO : item.getReceivedQuantity();
        return PurchaseOrderItemVO.builder()
                .id(item.getId())
                .materialId(item.getMaterialId())
                .materialCode(material == null ? null : material.getMaterialCode())
                .materialName(material == null ? null : material.getName())
                .specification(material == null ? null : material.getSpecification())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .receivedQuantity(received)
                .remainingQuantity(item.getQuantity().subtract(received))
                .remark(item.getRemark())
                .build();
    }

    private Map<Long, Supplier> loadSuppliers(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return supplierMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));
    }

    private Map<Long, Material> loadMaterials(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Material::getId, material -> material));
    }

    private Map<Long, List<PurchaseOrderItem>> loadItems(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                        .in(PurchaseOrderItem::getOrderId, orderIds)
                        .orderByAsc(PurchaseOrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(PurchaseOrderItem::getOrderId));
    }

    private String generateOrderNo() {
        String prefix = "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .likeRight(PurchaseOrder::getOrderNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}