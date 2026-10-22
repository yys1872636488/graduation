package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.ProductSku;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.mapper.ProductSkuMapper;
import org.example.graduation.service.ProductSkuService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {
    private final ProductSkuMapper mapper;
    private final ProductMapper productMapper;

    @Override
    public Page<ProductSku> list(Integer page, Integer size, Long productId, String keyword, Integer status) {
        LambdaQueryWrapper<ProductSku> w = new LambdaQueryWrapper<ProductSku>()
                .eq(productId != null, ProductSku::getProductId, productId)
                .eq(status != null, ProductSku::getStatus, status)
                .orderByDesc(ProductSku::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public ProductSku create(ProductSku sku) {
        if (sku.getProductId() == null || productMapper.selectById(sku.getProductId()) == null) {
            throw new BusinessException("关联产品不存在");
        }
        sku.setSkuCode(generateCode(sku.getProductId()));
        sku.setStatus(1);
        mapper.insert(sku);
        return sku;
    }

    @Override
    public ProductSku update(Long id, ProductSku sku) {
        ProductSku existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException("SKU不存在");
        existing.setProductId(sku.getProductId());
        existing.setColor(sku.getColor());
        existing.setSizeLabel(sku.getSizeLabel());
        existing.setUnit(sku.getUnit());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    public void toggleStatus(Long id) {
        ProductSku s = mapper.selectById(id);
        if (s == null) throw new BusinessException("SKU不存在");
        s.setStatus(s.getStatus() == 1 ? 0 : 1);
        mapper.updateById(s);
    }

    private String generateCode(Long productId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SKU-" + today + "-";
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<ProductSku>().likeRight(ProductSku::getSkuCode, prefix));
        return String.format("SKU-%s-%03d", today, (count != null ? count : 0) + 1);
    }
}