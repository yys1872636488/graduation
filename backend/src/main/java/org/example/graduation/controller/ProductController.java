package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.Product;
import org.example.graduation.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(service.list(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:write')")
    public Result<?> create(@RequestBody Product product) {
        return Result.success(service.create(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    public Result<?> update(@PathVariable Long id, @RequestBody Product product) {
        return Result.success(service.update(id, product));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('product:write')")
    public Result<?> toggleStatus(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.success();
    }
}