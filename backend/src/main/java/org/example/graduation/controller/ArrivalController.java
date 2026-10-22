package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.ArrivalCreateDTO;
import org.example.graduation.service.ArrivalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArrivalController {

    private final ArrivalService arrivalService;

    @GetMapping("/arrivals")
    @PreAuthorize("hasAuthority('arrival:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long orderId) {
        return Result.success(arrivalService.list(page, size, keyword, orderId));
    }

    @PostMapping("/purchase-orders/{orderId}/arrivals")
    @PreAuthorize("hasAuthority('arrival:register')")
    public Result<?> register(@PathVariable Long orderId,
                              @Valid @RequestBody ArrivalCreateDTO dto) {
        return Result.success(arrivalService.register(orderId, dto));
    }
}