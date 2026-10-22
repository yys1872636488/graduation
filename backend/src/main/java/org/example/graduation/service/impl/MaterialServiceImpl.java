package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.Material;
import org.example.graduation.mapper.MaterialMapper;
import org.example.graduation.service.MaterialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final MaterialMapper mapper;

    @Override
    public Page<Material> list(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<Material> w = new LambdaQueryWrapper<Material>()
                .and(StringUtils.hasText(keyword), q -> q
                    .like(Material::getName, keyword).or()
                    .like(Material::getCategory, keyword))
                .eq(status != null, Material::getStatus, status)
                .orderByDesc(Material::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Material create(Material material) {
        material.setMaterialCode(generateCode());
        material.setStatus(1);
        mapper.insert(material);
        return material;
    }

    @Override
    public Material update(Long id, Material material) {
        Material existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException("原材料不存在");
        existing.setName(material.getName());
        existing.setCategory(material.getCategory());
        existing.setSpecification(material.getSpecification());
        existing.setColor(material.getColor());
        existing.setUnit(material.getUnit());
        existing.setDefaultSupplierId(material.getDefaultSupplierId());
        existing.setRemark(material.getRemark());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    public void toggleStatus(Long id) {
        Material m = mapper.selectById(id);
        if (m == null) throw new BusinessException("原材料不存在");
        m.setStatus(m.getStatus() == 1 ? 0 : 1);
        mapper.updateById(m);
    }

    private String generateCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "MAT-" + today + "-";
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<Material>().likeRight(Material::getMaterialCode, prefix));
        return String.format("MAT-%s-%03d", today, (count != null ? count : 0) + 1);
    }
}