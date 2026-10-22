package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.AfterSalesCreateDTO;
import org.example.graduation.dto.AfterSalesProcessDTO;
import org.example.graduation.dto.SalesOrderSaveDTO;
import org.example.graduation.dto.ShipmentCreateDTO;
import org.example.graduation.vo.AfterSalesVO;
import org.example.graduation.vo.SalesOrderVO;
import org.example.graduation.vo.ShipmentVO;

public interface SalesService {

    Page<SalesOrderVO> listOrders(Integer page, Integer size, String keyword, String status);

    SalesOrderVO orderDetail(Long id);

    SalesOrderVO createOrder(SalesOrderSaveDTO dto);

    void cancelOrder(Long id);

    ShipmentVO createShipment(Long orderId, ShipmentCreateDTO dto);

    Page<ShipmentVO> listShipments(Integer page, Integer size, String keyword);

    Page<AfterSalesVO> listAfterSales(Integer page, Integer size, String keyword, String status);

    AfterSalesVO createAfterSales(AfterSalesCreateDTO dto);

    AfterSalesVO processAfterSales(Long id, AfterSalesProcessDTO dto);
}