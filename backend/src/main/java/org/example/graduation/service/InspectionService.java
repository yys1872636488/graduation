package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.InspectionReviewDTO;
import org.example.graduation.vo.InspectionDetailVO;
import org.example.graduation.vo.InspectionImageVO;
import org.example.graduation.vo.InspectionRecordQueryVO;
import org.example.graduation.vo.InspectionRecordVO;
import org.example.graduation.vo.VisionDetectionResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface InspectionService {

    InspectionImageVO uploadImage(Long batchId, MultipartFile file);

    void deleteImage(Long batchId, Long imageId);

    VisionDetectionResultVO detect(Long batchId, Long imageId);

    InspectionDetailVO detail(Long batchId);

    InspectionRecordVO review(Long batchId, InspectionReviewDTO dto);

    Page<InspectionRecordQueryVO> listRecords(Integer page, Integer size, String keyword,
                                              String conclusion, LocalDate startDate,
                                              LocalDate endDate);
}