package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.Product;
import org.example.graduation.mapper.ProductMapper;
import org.example.graduation.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductMapper mapper;

    @Override
    public Page<Product> list(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<Product>()
                .and(StringUtils.hasText(keyword), q -> q
                    .like(Product::getName, keyword).or()
                    .like(Product::getCategory, keyword))
                .eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Product create(Product product) {
        product.setProductCode(generateCode());
        product.setStatus(1);
        mapper.insert(product);
        return product;
    }

    @Override
    public Product update(Long id, Product product) {
        Product existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException("产品不存在");
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setStyle(product.getStyle());
        existing.setSeason(product.getSeason());
        existing.setDescription(product.getDescription());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    public void toggleStatus(Long id) {
        Product p = mapper.selectById(id);
        if (p == null) throw new BusinessException("产品不存在");
        p.setStatus(p.getStatus() == 1 ? 0 : 1);
        mapper.updateById(p);
    }

    private String generateCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PRD-" + today + "-";
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<Product>().likeRight(Product::getProductCode, prefix));
        return String.format("PRD-%s-%03d", today, (count != null ? count : 0) + 1);
    }
}