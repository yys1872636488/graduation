package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.PurchaseOrderSaveDTO;
import org.example.graduation.vo.PurchaseOrderVO;

public interface PurchaseOrderService {

    Page<PurchaseOrderVO> list(Integer page, Integer size, String keyword,
                               Long supplierId, String status);

    PurchaseOrderVO detail(Long id);

    PurchaseOrderVO create(PurchaseOrderSaveDTO dto);

    PurchaseOrderVO update(Long id, PurchaseOrderSaveDTO dto);

    void submit(Long id);

    void cancel(Long id);
}