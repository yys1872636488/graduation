package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.Material;
import org.example.graduation.service.MaterialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService service;

    @GetMapping
    @PreAuthorize("hasAuthority('material:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(service.list(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('material:write')")
    public Result<?> create(@RequestBody Material material) {
        return Result.success(service.create(material));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:write')")
    public Result<?> update(@PathVariable Long id, @RequestBody Material material) {
        return Result.success(service.update(id, material));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('material:write')")
    public Result<?> toggleStatus(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.success();
    }
}