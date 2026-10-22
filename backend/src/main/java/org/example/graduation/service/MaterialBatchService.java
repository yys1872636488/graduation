package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.vo.MaterialBatchVO;

public interface MaterialBatchService {

    Page<MaterialBatchVO> list(Integer page, Integer size, String keyword,
                               String inspectionStatus, Long supplierId);

    MaterialBatchVO detail(Long id);
}