package org.example.graduation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.dto.InspectionDetectDTO;
import org.example.graduation.dto.InspectionReviewDTO;
import org.example.graduation.service.InspectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/material-batches/{id}/inspection-images")
    @PreAuthorize("hasAuthority('inspection:upload')")
    public Result<?> uploadImage(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file) {
        return Result.success(inspectionService.uploadImage(id, file));
    }

    @DeleteMapping("/material-batches/{id}/inspection-images/{imageId}")
    @PreAuthorize("hasAuthority('inspection:upload')")
    public Result<?> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        inspectionService.deleteImage(id, imageId);
        return Result.success();
    }

    @PostMapping("/material-batches/{id}/detect")
    @PreAuthorize("hasAuthority('inspection:detect-btn')")
    public Result<?> detect(@PathVariable Long id,
                            @Valid @RequestBody InspectionDetectDTO dto) {
        return Result.success(inspectionService.detect(id, dto.getImageId()));
    }

    @GetMapping("/material-batches/{id}/inspection")
    @PreAuthorize("hasAnyAuthority('inspection:detect','inspection:record')")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(inspectionService.detail(id));
    }

    @GetMapping("/inspection-records")
    @PreAuthorize("hasAuthority('inspection:record')")
    public Result<?> records(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String conclusion,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(inspectionService.listRecords(
                page, size, keyword, conclusion, startDate, endDate));
    }

    @PostMapping("/material-batches/{id}/review")
    @PreAuthorize("hasAuthority('inspection:review-btn')")
    public Result<?> review(@PathVariable Long id,
                            @Valid @RequestBody InspectionReviewDTO dto) {
        return Result.success(inspectionService.review(id, dto));
    }
}