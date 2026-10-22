package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.FinishedInboundDTO;
import org.example.graduation.dto.MaterialInboundDTO;
import org.example.graduation.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Long warehouseId,
                          @RequestParam(required = false) String itemType,
                          @RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.list(page, size, warehouseId, itemType, keyword));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('inventory:transaction-read')")
    public Result<?> transactions(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) Long warehouseId,
                                  @RequestParam(required = false) String businessType,
                                  @RequestParam(required = false) String keyword) {
        return Result.success(inventoryService.listTransactions(
                page, size, warehouseId, businessType, keyword));
    }

    @PostMapping("/material-inbound")
    @PreAuthorize("hasAuthority('inventory:inbound-material')")
    public Result<?> materialInbound(@Valid @RequestBody MaterialInboundDTO dto) {
        return Result.success(inventoryService.materialInbound(
                dto.getBatchId(), dto.getWarehouseId()));
    }

    @PostMapping("/finished-inbound")
    @PreAuthorize("hasAuthority('inventory:inbound-finished')")
    public Result<?> finishedInbound(@Valid @RequestBody FinishedInboundDTO dto) {
        return Result.success(inventoryService.finishedInbound(
                dto.getFinishedBatchId(), dto.getWarehouseId()));
    }
}