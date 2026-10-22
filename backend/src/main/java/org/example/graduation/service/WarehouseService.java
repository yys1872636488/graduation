package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.entity.Warehouse;

public interface WarehouseService {
    Page<Warehouse> list(Integer page, Integer size, String keyword, Integer status);
    Warehouse create(Warehouse warehouse);
    Warehouse update(Long id, Warehouse warehouse);
    void toggleStatus(Long id);
}