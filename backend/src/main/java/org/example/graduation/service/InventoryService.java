package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.vo.InventoryTransactionVO;
import org.example.graduation.vo.InventoryVO;

import java.math.BigDecimal;

public interface InventoryService {

    Page<InventoryVO> list(Integer page, Integer size, Long warehouseId,
                           String itemType, String keyword);

    Page<InventoryTransactionVO> listTransactions(Integer page, Integer size,
                                                  Long warehouseId, String businessType,
                                                  String keyword);

    InventoryVO materialInbound(Long materialBatchId, Long warehouseId);

    InventoryVO finishedInbound(Long finishedBatchId, Long warehouseId);

    void issueMaterial(Long materialBatchId, BigDecimal quantity,
                       String businessNo, String remark);

    void salesOutbound(Long finishedBatchId, BigDecimal quantity,
                       String businessNo, String remark);
}