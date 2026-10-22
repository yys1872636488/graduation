package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.ProductionStatus;
import org.example.graduation.common.SalesStatus;
import org.example.graduation.dto.AfterSalesCreateDTO;
import org.example.graduation.dto.AfterSalesProcessDTO;
import org.example.graduation.dto.SalesOrderItemDTO;
import org.example.graduation.dto.SalesOrderSaveDTO;
import org.example.graduation.dto.ShipmentCreateDTO;
import org.example.graduation.dto.ShipmentItemDTO;
import org.example.graduation.entity.AfterSalesRecord;
import org.example.graduation.entity.FinishedBatch;
import org.example.graduation.entity.Product;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.entity.SalesOrder;
import org.example.graduation.entity.SalesOrderItem;
import org.example.graduation.entity.Shipment;
import org.example.graduation.entity.ShipmentItem;
import org.example.graduation.mapper.AfterSalesRecordMapper;
import org.example.graduation.mapper.FinishedBatchMapper;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.mapper.ProductSkuMapper;
import org.example.graduation.mapper.SalesOrderItemMapper;
import org.example.graduation.mapper.SalesOrderMapper;
import org.example.graduation.mapper.ShipmentItemMapper;
import org.example.graduation.mapper.ShipmentMapper;
import org.example.graduation.security.CurrentUserProvider;
import org.example.graduation.service.InventoryService;
import org.example.graduation.service.SalesService;
import org.example.graduation.vo.AfterSalesVO;
import org.example.graduation.vo.SalesOrderItemVO;
import org.example.graduation.vo.SalesOrderVO;
import org.example.graduation.vo.ShipmentItemVO;
import org.example.graduation.vo.ShipmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private static final Set<String> AFTER_TYPES = Set.of(
            SalesStatus.TYPE_RETURN, SalesStatus.TYPE_EXCHANGE,
            SalesStatus.TYPE_REPAIR, SalesStatus.TYPE_CONSULT);
    private static final Set<String> AFTER_STATUSES = Set.of(
            SalesStatus.AFTER_PENDING, SalesStatus.AFTER_PROCESSING,
            SalesStatus.AFTER_PENDING_ACCEPTANCE, SalesStatus.AFTER_COMPLETED,
            SalesStatus.AFTER_CLOSED);

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentItemMapper shipmentItemMapper;
    private final AfterSalesRecordMapper afterSalesMapper;
    private final FinishedBatchMapper finishedBatchMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<SalesOrderVO> listOrders(Integer page, Integer size,
                                         String keyword, String status) {
        Page<SalesOrder> orderPage = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SalesOrder>()
                        .like(StringUtils.hasText(keyword), SalesOrder::getOrderNo, keyword)
                        .eq(StringUtils.hasText(status), SalesOrder::getStatus, status)
                        .orderByDesc(SalesOrder::getCreatedAt));
        List<SalesOrder> records = orderPage.getRecords();
        Map<Long, List<SalesOrderItem>> itemsByOrder = loadOrderItems(
                records.stream().map(SalesOrder::getId).toList());
        Map<Long, ProductSku> skus = loadSkus(itemsByOrder.values().stream()
                .flatMap(List::stream).map(SalesOrderItem::getSkuId).toList());
        Map<Long, Product> products = loadProducts(skus.values().stream()
                .map(ProductSku::getProductId).toList());

        List<SalesOrderVO> vos = records.stream()
                .map(order -> toOrderVO(order,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        skus, products, false))
                .toList();
        Page<SalesOrderVO> result = new Page<>(
                orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public SalesOrderVO orderDetail(Long id) {
        SalesOrder order = requireOrder(id);
        List<SalesOrderItem> items = loadOrderItems(List.of(id))
                .getOrDefault(id, List.of());
        Map<Long, ProductSku> skus = loadSkus(
                items.stream().map(SalesOrderItem::getSkuId).toList());
        Map<Long, Product> products = loadProducts(skus.values().stream()
                .map(ProductSku::getProductId).toList());
        return toOrderVO(order, items, skus, products, true);
    }

    @Override
    @Transactional
    public SalesOrderVO createOrder(SalesOrderSaveDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("请至少添加一条订单明细");
        }
        Set<Long> seenSkus = new HashSet<>();
        for (SalesOrderItemDTO item : dto.getItems()) {
            ProductSku sku = skuMapper.selectById(item.getSkuId());
            if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                throw new BusinessException("SKU不存在或已停用");
            }
            if (!seenSkus.add(item.getSkuId())) {
                throw new BusinessException("同一SKU不能重复添加，请合并数量");
            }
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomerName(dto.getCustomerName());
        order.setOrderDate(dto.getOrderDate());
        order.setStatus(SalesStatus.ORDER_PENDING_SHIPMENT);
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        order.setCreatedBy(currentUserProvider.getUserId());
        orderMapper.insert(order);

        for (SalesOrderItemDTO dtoItem : dto.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(dtoItem.getSkuId());
            item.setQuantity(dtoItem.getQuantity());
            item.setUnitPrice(dtoItem.getUnitPrice());
            item.setShippedQuantity(BigDecimal.ZERO);
            item.setRemark(dtoItem.getRemark() == null ? "" : dtoItem.getRemark());
            orderItemMapper.insert(item);
        }
        return orderDetail(order.getId());
    }

    @Override
    public void cancelOrder(Long id) {
        SalesOrder order = requireOrder(id);
        if (!SalesStatus.ORDER_PENDING_SHIPMENT.equals(order.getStatus())) {
            throw new BusinessException("只有待发货状态的销售订单可以取消");
        }
        Long shipmentCount = shipmentMapper.selectCount(
                new LambdaQueryWrapper<Shipment>().eq(Shipment::getOrderId, id));
        if (shipmentCount != null && shipmentCount > 0) {
            throw new BusinessException("订单已有发货记录，不能取消");
        }
        order.setStatus(SalesStatus.ORDER_CANCELLED);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public ShipmentVO createShipment(Long orderId, ShipmentCreateDTO dto) {
        SalesOrder order = requireOrder(orderId);
        if (!SalesStatus.ORDER_PENDING_SHIPMENT.equals(order.getStatus())
                && !SalesStatus.ORDER_PARTIAL_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不能发货");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("请至少添加一条发货明细");
        }

        List<SalesOrderItem> orderItems = loadOrderItems(List.of(orderId))
                .getOrDefault(orderId, List.of());
        Map<Long, SalesOrderItem> itemBySku = orderItems.stream()
                .collect(Collectors.toMap(SalesOrderItem::getSkuId, item -> item));

        Shipment shipment = new Shipment();
        shipment.setShipmentNo(generateShipmentNo());
        shipment.setOrderId(orderId);
        shipment.setShipmentDate(dto.getShipmentDate());
        shipment.setLogisticsNo(dto.getLogisticsNo() == null ? "" : dto.getLogisticsNo());
        shipment.setOperator(dto.getOperator() == null ? "" : dto.getOperator());
        shipment.setStatus("SHIPPED");
        shipment.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        shipment.setCreatedBy(currentUserProvider.getUserId());
        shipmentMapper.insert(shipment);

        for (ShipmentItemDTO dtoItem : dto.getItems()) {
            FinishedBatch batch = finishedBatchMapper.selectById(dtoItem.getFinishedBatchId());
            if (batch == null || !ProductionStatus.BATCH_AVAILABLE.equals(batch.getStatus())) {
                throw new BusinessException("成品批次不存在或不在可售库存状态");
            }
            SalesOrderItem orderItem = itemBySku.get(batch.getSkuId());
            if (orderItem == null) {
                throw new BusinessException("发货批次SKU不在销售订单中");
            }
            BigDecimal remaining = orderItem.getQuantity()
                    .subtract(orderItem.getShippedQuantity() == null
                            ? BigDecimal.ZERO : orderItem.getShippedQuantity());
            if (dtoItem.getQuantity().compareTo(remaining) > 0) {
                throw new BusinessException("发货数量不能超过订单剩余待发数量");
            }

            inventoryService.salesOutbound(batch.getId(), dtoItem.getQuantity(),
                    shipment.getShipmentNo(), "销售发货");

            ShipmentItem item = new ShipmentItem();
            item.setShipmentId(shipment.getId());
            item.setFinishedBatchId(batch.getId());
            item.setSkuId(batch.getSkuId());
            item.setQuantity(dtoItem.getQuantity());
            item.setRemark(dtoItem.getRemark() == null ? "" : dtoItem.getRemark());
            shipmentItemMapper.insert(item);

            orderItem.setShippedQuantity(
                    (orderItem.getShippedQuantity() == null
                            ? BigDecimal.ZERO : orderItem.getShippedQuantity())
                            .add(dtoItem.getQuantity()));
            orderItemMapper.updateById(orderItem);
        }

        List<SalesOrderItem> refreshed = loadOrderItems(List.of(orderId))
                .getOrDefault(orderId, List.of());
        boolean allShipped = refreshed.stream().allMatch(item ->
                item.getShippedQuantity().compareTo(item.getQuantity()) >= 0);
        order.setStatus(allShipped
                ? SalesStatus.ORDER_SHIPPED : SalesStatus.ORDER_PARTIAL_SHIPPED);
        orderMapper.updateById(order);

        return toShipmentVO(shipment, order, null);
    }

    @Override
    public Page<ShipmentVO> listShipments(Integer page, Integer size, String keyword) {
        Page<Shipment> shipmentPage = shipmentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Shipment>()
                        .like(StringUtils.hasText(keyword), Shipment::getShipmentNo, keyword)
                        .orderByDesc(Shipment::getCreatedAt));
        List<Shipment> records = shipmentPage.getRecords();
        Map<Long, SalesOrder> orders = loadOrders(
                records.stream().map(Shipment::getOrderId).toList());
        List<ShipmentVO> vos = records.stream()
                .map(item -> toShipmentVO(item, orders.get(item.getOrderId()), null))
                .toList();
        Page<ShipmentVO> result = new Page<>(
                shipmentPage.getCurrent(), shipmentPage.getSize(), shipmentPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public Page<AfterSalesVO> listAfterSales(Integer page, Integer size,
                                             String keyword, String status) {
        Page<AfterSalesRecord> recordPage = afterSalesMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AfterSalesRecord>()
                        .like(StringUtils.hasText(keyword), AfterSalesRecord::getRecordNo, keyword)
                        .eq(StringUtils.hasText(status), AfterSalesRecord::getStatus, status)
                        .orderByDesc(AfterSalesRecord::getCreatedAt));
        List<AfterSalesRecord> records = recordPage.getRecords();
        Map<Long, SalesOrder> orders = loadOrders(
                records.stream().map(AfterSalesRecord::getSalesOrderId).toList());
        Map<Long, Shipment> shipments = loadShipments(
                records.stream().map(AfterSalesRecord::getShipmentId).toList());
        Map<Long, FinishedBatch> batches = loadFinishedBatches(
                records.stream().map(AfterSalesRecord::getFinishedBatchId).toList());
        List<AfterSalesVO> vos = records.stream()
                .map(item -> toAfterSalesVO(item,
                        orders.get(item.getSalesOrderId()),
                        shipments.get(item.getShipmentId()),
                        batches.get(item.getFinishedBatchId())))
                .toList();
        Page<AfterSalesVO> result = new Page<>(
                recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public AfterSalesVO createAfterSales(AfterSalesCreateDTO dto) {
        String type = dto.getType() == null ? "" : dto.getType().toUpperCase();
        if (!AFTER_TYPES.contains(type)) {
            throw new BusinessException("售后类型不合法");
        }
        SalesOrder order = requireOrder(dto.getSalesOrderId());
        Shipment shipment = null;
        if (dto.getShipmentId() != null) {
            shipment = shipmentMapper.selectById(dto.getShipmentId());
            if (shipment == null || !order.getId().equals(shipment.getOrderId())) {
                throw new BusinessException("发货单不属于该销售订单");
            }
        }
        FinishedBatch batch = null;
        if (dto.getFinishedBatchId() != null) {
            batch = finishedBatchMapper.selectById(dto.getFinishedBatchId());
            if (batch == null) {
                throw new BusinessException("成品批次不存在");
            }
        }

        AfterSalesRecord record = new AfterSalesRecord();
        record.setRecordNo(generateAfterSalesNo());
        record.setSalesOrderId(order.getId());
        record.setShipmentId(dto.getShipmentId());
        record.setFinishedBatchId(dto.getFinishedBatchId());
        record.setType(type);
        record.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        record.setApplyDate(dto.getApplyDate());
        record.setStatus(SalesStatus.AFTER_PENDING);
        record.setConclusion("");
        record.setAcceptanceResult("");
        record.setHandler(dto.getHandler() == null ? "" : dto.getHandler());
        record.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        afterSalesMapper.insert(record);
        return toAfterSalesVO(record, order, shipment, batch);
    }

    @Override
    public AfterSalesVO processAfterSales(Long id, AfterSalesProcessDTO dto) {
        AfterSalesRecord record = afterSalesMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("售后记录不存在");
        }
        String status = dto.getStatus() == null ? "" : dto.getStatus().toUpperCase();
        if (!AFTER_STATUSES.contains(status)) {
            throw new BusinessException("售后状态不合法");
        }
        record.setStatus(status);
        record.setConclusion(dto.getConclusion() == null ? "" : dto.getConclusion());
        record.setAcceptanceResult(dto.getAcceptanceResult() == null
                ? "" : dto.getAcceptanceResult());
        record.setHandler(dto.getHandler() == null ? record.getHandler() : dto.getHandler());
        record.setRemark(dto.getRemark() == null ? record.getRemark() : dto.getRemark());
        afterSalesMapper.updateById(record);

        return toAfterSalesVO(record,
                orderMapper.selectById(record.getSalesOrderId()),
                record.getShipmentId() == null ? null
                        : shipmentMapper.selectById(record.getShipmentId()),
                record.getFinishedBatchId() == null ? null
                        : finishedBatchMapper.selectById(record.getFinishedBatchId()));
    }

    // ==========================================================
    // 装配
    // ==========================================================

    private SalesOrderVO toOrderVO(SalesOrder order, List<SalesOrderItem> items,
                                   Map<Long, ProductSku> skus, Map<Long, Product> products,
                                   boolean withItems) {
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice() == null
                        ? BigDecimal.ZERO
                        : item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalesOrderItemVO> itemVOs = withItems ? items.stream().map(item -> {
            ProductSku sku = skus.get(item.getSkuId());
            Product product = sku == null ? null : products.get(sku.getProductId());
            BigDecimal shipped = item.getShippedQuantity() == null
                    ? BigDecimal.ZERO : item.getShippedQuantity();
            return SalesOrderItemVO.builder()
                    .id(item.getId())
                    .skuId(item.getSkuId())
                    .skuCode(sku == null ? null : sku.getSkuCode())
                    .productName(product == null ? null : product.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .shippedQuantity(shipped)
                    .remainingQuantity(item.getQuantity().subtract(shipped))
                    .remark(item.getRemark())
                    .build();
        }).toList() : null;

        return SalesOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerName(order.getCustomerName())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .itemCount(items.size())
                .totalAmount(total)
                .items(itemVOs)
                .build();
    }

    private ShipmentVO toShipmentVO(Shipment shipment, SalesOrder order,
                                    List<ShipmentItem> ignored) {
        List<ShipmentItem> items = shipmentItemMapper.selectList(
                new LambdaQueryWrapper<ShipmentItem>()
                        .eq(ShipmentItem::getShipmentId, shipment.getId()));
        Map<Long, FinishedBatch> batches = loadFinishedBatches(
                items.stream().map(ShipmentItem::getFinishedBatchId).toList());
        Map<Long, ProductSku> skus = loadSkus(
                items.stream().map(ShipmentItem::getSkuId).toList());
        List<ShipmentItemVO> itemVOs = items.stream().map(item -> {
            FinishedBatch batch = batches.get(item.getFinishedBatchId());
            ProductSku sku = skus.get(item.getSkuId());
            return ShipmentItemVO.builder()
                    .id(item.getId())
                    .finishedBatchId(item.getFinishedBatchId())
                    .batchNo(batch == null ? null : batch.getBatchNo())
                    .skuId(item.getSkuId())
                    .skuCode(sku == null ? null : sku.getSkuCode())
                    .quantity(item.getQuantity())
                    .remark(item.getRemark())
                    .build();
        }).toList();
        return ShipmentVO.builder()
                .id(shipment.getId())
                .shipmentNo(shipment.getShipmentNo())
                .orderId(shipment.getOrderId())
                .orderNo(order == null ? null : order.getOrderNo())
                .customerName(order == null ? null : order.getCustomerName())
                .shipmentDate(shipment.getShipmentDate())
                .logisticsNo(shipment.getLogisticsNo())
                .operator(shipment.getOperator())
                .status(shipment.getStatus())
                .remark(shipment.getRemark())
                .createdAt(shipment.getCreatedAt())
                .items(itemVOs)
                .build();
    }

    private AfterSalesVO toAfterSalesVO(AfterSalesRecord record, SalesOrder order,
                                        Shipment shipment, FinishedBatch batch) {
        return AfterSalesVO.builder()
                .id(record.getId())
                .recordNo(record.getRecordNo())
                .salesOrderId(record.getSalesOrderId())
                .orderNo(order == null ? null : order.getOrderNo())
                .shipmentId(record.getShipmentId())
                .shipmentNo(shipment == null ? null : shipment.getShipmentNo())
                .finishedBatchId(record.getFinishedBatchId())
                .finishedBatchNo(batch == null ? null : batch.getBatchNo())
                .type(record.getType())
                .description(record.getDescription())
                .applyDate(record.getApplyDate())
                .status(record.getStatus())
                .conclusion(record.getConclusion())
                .acceptanceResult(record.getAcceptanceResult())
                .handler(record.getHandler())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private SalesOrder requireOrder(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("销售订单不存在");
        }
        return order;
    }

    private Map<Long, List<SalesOrderItem>> loadOrderItems(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                        .in(SalesOrderItem::getOrderId, orderIds)
                        .orderByAsc(SalesOrderItem::getId))
                .stream().collect(Collectors.groupingBy(SalesOrderItem::getOrderId));
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

    private Map<Long, SalesOrder> loadOrders(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SalesOrder::getId, order -> order));
    }

    private Map<Long, Shipment> loadShipments(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return shipmentMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Shipment::getId, shipment -> shipment));
    }

    private Map<Long, FinishedBatch> loadFinishedBatches(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        return finishedBatchMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(FinishedBatch::getId, batch -> batch));
    }

    private String generateOrderNo() {
        String prefix = "SO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .likeRight(SalesOrder::getOrderNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateShipmentNo() {
        String prefix = "SH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = shipmentMapper.selectCount(new LambdaQueryWrapper<Shipment>()
                .likeRight(Shipment::getShipmentNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private String generateAfterSalesNo() {
        String prefix = "AS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = afterSalesMapper.selectCount(new LambdaQueryWrapper<AfterSalesRecord>()
                .likeRight(AfterSalesRecord::getRecordNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }
}