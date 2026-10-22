package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.entity.ProductSku;

public interface ProductSkuService {
    Page<ProductSku> list(Integer page, Integer size, Long productId, String keyword, Integer status);
    ProductSku create(ProductSku sku);
    ProductSku update(Long id, ProductSku sku);
    void toggleStatus(Long id);
}