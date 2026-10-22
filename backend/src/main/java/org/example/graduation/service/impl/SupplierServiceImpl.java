package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.Supplier;
import org.example.graduation.mapper.SupplierMapper;
import org.example.graduation.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierMapper mapper;

    @Override
    public Page<Supplier> list(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<Supplier>()
                .and(StringUtils.hasText(keyword), q -> q
                    .like(Supplier::getName, keyword).or()
                    .like(Supplier::getContactPerson, keyword))
                .eq(status != null, Supplier::getStatus, status)
                .orderByDesc(Supplier::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Supplier create(Supplier supplier) {
        supplier.setSupplierCode(generateCode());
        supplier.setStatus(1);
        mapper.insert(supplier);
        return supplier;
    }

    @Override
    public Supplier update(Long id, Supplier supplier) {
        Supplier existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException("供应商不存在");
        existing.setName(supplier.getName());
        existing.setContactPerson(supplier.getContactPerson());
        existing.setPhone(supplier.getPhone());
        existing.setAddress(supplier.getAddress());
        existing.setRemark(supplier.getRemark());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    public void toggleStatus(Long id) {
        Supplier s = mapper.selectById(id);
        if (s == null) throw new BusinessException("供应商不存在");
        s.setStatus(s.getStatus() == 1 ? 0 : 1);
        mapper.updateById(s);
    }

    private String generateCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SUP-" + today + "-";
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<Supplier>().likeRight(Supplier::getSupplierCode, prefix));
        return String.format("SUP-%s-%03d", today, (count != null ? count : 0) + 1);
    }
}