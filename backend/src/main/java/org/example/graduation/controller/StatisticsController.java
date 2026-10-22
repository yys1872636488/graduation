package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('statistics:read')")
    public Result<?> overview() {
        return Result.success(statisticsService.overview());
    }

    @GetMapping("/quality")
    @PreAuthorize("hasAuthority('statistics:read')")
    public Result<?> quality() {
        return Result.success(statisticsService.quality());
    }
}