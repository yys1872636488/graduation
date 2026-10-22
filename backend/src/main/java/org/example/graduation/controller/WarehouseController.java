package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.Warehouse;
import org.example.graduation.service.WarehouseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService service;

    @GetMapping
    @PreAuthorize("hasAuthority('warehouse:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(service.list(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('warehouse:write')")
    public Result<?> create(@RequestBody Warehouse warehouse) {
        return Result.success(service.create(warehouse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('warehouse:write')")
    public Result<?> update(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        return Result.success(service.update(id, warehouse));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('warehouse:write')")
    public Result<?> toggleStatus(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.success();
    }
}