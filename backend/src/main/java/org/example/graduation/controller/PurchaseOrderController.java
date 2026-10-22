package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.PurchaseOrderSaveDTO;
import org.example.graduation.service.PurchaseOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long supplierId,
                          @RequestParam(required = false) String status) {
        return Result.success(purchaseOrderService.list(page, size, keyword, supplierId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:read')")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(purchaseOrderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create')")
    public Result<?> create(@Valid @RequestBody PurchaseOrderSaveDTO dto) {
        return Result.success(purchaseOrderService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderSaveDTO dto) {
        return Result.success(purchaseOrderService.update(id, dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('purchase:submit')")
    public Result<?> submit(@PathVariable Long id) {
        purchaseOrderService.submit(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:edit')")
    public Result<?> cancel(@PathVariable Long id) {
        purchaseOrderService.cancel(id);
        return Result.success();
    }
}