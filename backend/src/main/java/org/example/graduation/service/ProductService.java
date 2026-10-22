package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.entity.Product;

public interface ProductService {
    Page<Product> list(Integer page, Integer size, String keyword, Integer status);
    Product create(Product product);
    Product update(Long id, Product product);
    void toggleStatus(Long id);
}