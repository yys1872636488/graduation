package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.service.ProductSkuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class ProductSkuController {
    private final ProductSkuService service;

    @GetMapping
    @PreAuthorize("hasAuthority('sku:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Long productId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(service.list(page, size, productId, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sku:write')")
    public Result<?> create(@RequestBody ProductSku sku) {
        return Result.success(service.create(sku));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sku:write')")
    public Result<?> update(@PathVariable Long id, @RequestBody ProductSku sku) {
        return Result.success(service.update(id, sku));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('sku:write')")
    public Result<?> toggleStatus(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.success();
    }
}