package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.ProductionPlanReleaseDTO;
import org.example.graduation.dto.ProductionPlanSaveDTO;
import org.example.graduation.service.ProductionPlanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production-plans")
@RequiredArgsConstructor
public class ProductionPlanController {

    private final ProductionPlanService productionPlanService;

    @GetMapping
    @PreAuthorize("hasAuthority('production:plan:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status) {
        return Result.success(productionPlanService.list(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('production:plan:create')")
    public Result<?> create(@Valid @RequestBody ProductionPlanSaveDTO dto) {
        return Result.success(productionPlanService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('production:plan:edit')")
    public Result<?> update(@PathVariable Long id,
                            @Valid @RequestBody ProductionPlanSaveDTO dto) {
        return Result.success(productionPlanService.update(id, dto));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('production:plan:release')")
    public Result<?> release(@PathVariable Long id,
                             @RequestBody(required = false) ProductionPlanReleaseDTO dto) {
        return Result.success(productionPlanService.release(
                id, dto == null ? null : dto.getQuantity()));
    }

    @GetMapping("/{id}/status-logs")
    @PreAuthorize("hasAuthority('production:plan:read')")
    public Result<?> statusLogs(@PathVariable Long id) {
        return Result.success(productionPlanService.statusLogs(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('production:plan:edit')")
    public Result<?> cancel(@PathVariable Long id) {
        productionPlanService.cancel(id);
        return Result.success();
    }
}