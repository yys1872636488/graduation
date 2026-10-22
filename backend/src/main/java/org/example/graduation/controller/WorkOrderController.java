package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.FinishedBatchCreateDTO;
import org.example.graduation.dto.FinishedInspectionDTO;
import org.example.graduation.dto.MaterialIssueCreateDTO;
import org.example.graduation.dto.ProductionProgressDTO;
import org.example.graduation.dto.QualityDispositionDTO;
import org.example.graduation.dto.WorkOrderCancelDTO;
import org.example.graduation.dto.WorkOrderSaveDTO;
import org.example.graduation.service.WorkOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping("/work-orders")
    @PreAuthorize("hasAuthority('production:order:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long planId) {
        return Result.success(workOrderService.list(page, size, keyword, status, planId));
    }

    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasAuthority('production:order:read')")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(workOrderService.detail(id));
    }

    @PostMapping("/work-orders")
    @PreAuthorize("hasAuthority('production:order:create')")
    public Result<?> create(@Valid @RequestBody WorkOrderSaveDTO dto) {
        return Result.success(workOrderService.create(dto));
    }

    @PostMapping("/work-orders/{id}/start")
    @PreAuthorize("hasAuthority('production:order:start')")
    public Result<?> start(@PathVariable Long id) {
        workOrderService.start(id);
        return Result.success();
    }

    @PostMapping("/work-orders/{id}/cancel")
    @PreAuthorize("hasAuthority('production:order:cancel')")
    public Result<?> cancel(@PathVariable Long id,
                            @RequestBody(required = false) WorkOrderCancelDTO dto) {
        workOrderService.cancel(id, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @GetMapping("/work-orders/{id}/status-logs")
    @PreAuthorize("hasAuthority('production:order:read')")
    public Result<?> statusLogs(@PathVariable Long id) {
        return Result.success(workOrderService.statusLogs(id));
    }

    @PostMapping("/work-orders/{id}/material-issues")
    @PreAuthorize("hasAuthority('production:issue-material')")
    public Result<?> createIssue(@PathVariable Long id,
                                 @Valid @RequestBody MaterialIssueCreateDTO dto) {
        return Result.success(workOrderService.createIssue(id, dto));
    }

    @GetMapping("/material-issues")
    @PreAuthorize("hasAuthority('production:order:read')")
    public Result<?> listIssues(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long workOrderId) {
        return Result.success(workOrderService.listIssues(page, size, keyword, workOrderId));
    }

    @PostMapping("/work-orders/{id}/progress")
    @PreAuthorize("hasAuthority('production:order:report')")
    public Result<?> addProgress(@PathVariable Long id,
                                 @Valid @RequestBody ProductionProgressDTO dto) {
        return Result.success(workOrderService.addProgress(id, dto));
    }

    @PostMapping("/work-orders/{id}/finished-inspections")
    @PreAuthorize("hasAuthority('production:qc-write')")
    public Result<?> addInspection(@PathVariable Long id,
                                   @Valid @RequestBody FinishedInspectionDTO dto) {
        return Result.success(workOrderService.addInspection(id, dto));
    }

    @GetMapping("/finished-inspections")
    @PreAuthorize("hasAuthority('production:order:read')")
    public Result<?> listInspections(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(workOrderService.listInspections(page, size, keyword));
    }

    @PostMapping("/quality-dispositions")
    @PreAuthorize("hasAuthority('production:qc-review')")
    public Result<?> addDisposition(@Valid @RequestBody QualityDispositionDTO dto) {
        return Result.success(workOrderService.addDisposition(dto));
    }

    @PostMapping("/work-orders/{id}/finished-batches")
    @PreAuthorize("hasAuthority('production:qc-write')")
    public Result<?> createBatch(@PathVariable Long id,
                                 @RequestBody(required = false) FinishedBatchCreateDTO dto) {
        return Result.success(workOrderService.createBatch(
                id, dto == null ? new FinishedBatchCreateDTO() : dto));
    }

    @GetMapping("/finished-batches")
    @PreAuthorize("hasAnyAuthority('production:order:read','inventory:read','sales:read')")
    public Result<?> listBatches(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String status) {
        return Result.success(workOrderService.listBatches(page, size, keyword, status));
    }
}