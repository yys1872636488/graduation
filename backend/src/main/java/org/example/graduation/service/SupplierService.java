package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.entity.Supplier;

public interface SupplierService {
    Page<Supplier> list(Integer page, Integer size, String keyword, Integer status);
    Supplier create(Supplier supplier);
    Supplier update(Long id, Supplier supplier);
    void toggleStatus(Long id);
}