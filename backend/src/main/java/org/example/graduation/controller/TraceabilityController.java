package org.example.graduation.controller;

import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.service.TraceabilityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traceability")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @GetMapping("/material-batches/{batchNo}")
    @PreAuthorize("hasAuthority('traceability:read')")
    public Result<?> materialBatch(@PathVariable String batchNo) {
        return Result.success(traceabilityService.traceMaterialBatch(batchNo));
    }

    @GetMapping("/finished-batches/{batchNo}")
    @PreAuthorize("hasAuthority('traceability:read')")
    public Result<?> finishedBatch(@PathVariable String batchNo) {
        return Result.success(traceabilityService.traceFinishedBatch(batchNo));
    }
}