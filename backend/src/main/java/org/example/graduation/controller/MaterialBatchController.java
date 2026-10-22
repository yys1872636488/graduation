package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.service.MaterialBatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material-batches")
@RequiredArgsConstructor
public class MaterialBatchController {

    private final MaterialBatchService materialBatchService;

    @GetMapping
    @PreAuthorize("hasAuthority('batch:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String inspectionStatus,
                          @RequestParam(required = false) Long supplierId) {
        return Result.success(materialBatchService.list(
                page, size, keyword, inspectionStatus, supplierId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('batch:read')")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(materialBatchService.detail(id));
    }
}