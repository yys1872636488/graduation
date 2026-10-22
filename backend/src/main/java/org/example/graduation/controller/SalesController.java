package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.AfterSalesCreateDTO;
import org.example.graduation.dto.AfterSalesProcessDTO;
import org.example.graduation.dto.SalesOrderSaveDTO;
import org.example.graduation.dto.ShipmentCreateDTO;
import org.example.graduation.service.SalesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping("/sales-orders")
    @PreAuthorize("hasAuthority('sales:read')")
    public Result<?> listOrders(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String status) {
        return Result.success(salesService.listOrders(page, size, keyword, status));
    }

    @GetMapping("/sales-orders/{id}")
    @PreAuthorize("hasAuthority('sales:read')")
    public Result<?> orderDetail(@PathVariable Long id) {
        return Result.success(salesService.orderDetail(id));
    }

    @PostMapping("/sales-orders")
    @PreAuthorize("hasAuthority('sales:create')")
    public Result<?> createOrder(@Valid @RequestBody SalesOrderSaveDTO dto) {
        return Result.success(salesService.createOrder(dto));
    }

    @PostMapping("/sales-orders/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:edit')")
    public Result<?> cancelOrder(@PathVariable Long id) {
        salesService.cancelOrder(id);
        return Result.success();
    }

    @PostMapping("/sales-orders/{id}/shipments")
    @PreAuthorize("hasAuthority('sales:ship')")
    public Result<?> createShipment(@PathVariable Long id,
                                    @Valid @RequestBody ShipmentCreateDTO dto) {
        return Result.success(salesService.createShipment(id, dto));
    }

    @GetMapping("/shipments")
    @PreAuthorize("hasAuthority('sales:read')")
    public Result<?> listShipments(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String keyword) {
        return Result.success(salesService.listShipments(page, size, keyword));
    }

    @GetMapping("/after-sales")
    @PreAuthorize("hasAuthority('after-sales:read')")
    public Result<?> listAfterSales(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status) {
        return Result.success(salesService.listAfterSales(page, size, keyword, status));
    }

    @PostMapping("/after-sales")
    @PreAuthorize("hasAuthority('after-sales:handle')")
    public Result<?> createAfterSales(@Valid @RequestBody AfterSalesCreateDTO dto) {
        return Result.success(salesService.createAfterSales(dto));
    }

    @PostMapping("/after-sales/{id}/process")
    @PreAuthorize("hasAuthority('after-sales:handle')")
    public Result<?> processAfterSales(@PathVariable Long id,
                                       @Valid @RequestBody AfterSalesProcessDTO dto) {
        return Result.success(salesService.processAfterSales(id, dto));
    }
}