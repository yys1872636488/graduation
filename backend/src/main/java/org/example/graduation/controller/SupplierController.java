package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.Supplier;
import org.example.graduation.service.SupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService service;

    @GetMapping
    @PreAuthorize("hasAuthority('supplier:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(service.list(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier:write')")
    public Result<?> create(@RequestBody Supplier supplier) {
        return Result.success(service.create(supplier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:write')")
    public Result<?> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        return Result.success(service.update(id, supplier));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('supplier:write')")
    public Result<?> toggleStatus(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.success();
    }
}