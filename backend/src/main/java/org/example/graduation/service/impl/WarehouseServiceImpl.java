package org.example.graduation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.entity.Warehouse;
import org.example.graduation.mapper.WarehouseMapper;
import org.example.graduation.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseMapper mapper;

    @Override
    public Page<Warehouse> list(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<Warehouse> w = new LambdaQueryWrapper<Warehouse>()
                .and(StringUtils.hasText(keyword), q -> q
                    .like(Warehouse::getName, keyword).or()
                    .like(Warehouse::getLocation, keyword))
                .eq(status != null, Warehouse::getStatus, status)
                .orderByDesc(Warehouse::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        warehouse.setWarehouseCode(generateCode());
        warehouse.setStatus(1);
        mapper.insert(warehouse);
        return warehouse;
    }

    @Override
    public Warehouse update(Long id, Warehouse warehouse) {
        Warehouse existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException("仓库不存在");
        existing.setName(warehouse.getName());
        existing.setLocation(warehouse.getLocation());
        existing.setRemark(warehouse.getRemark());
        mapper.updateById(existing);
        return existing;
    }

    @Override
    public void toggleStatus(Long id) {
        Warehouse wh = mapper.selectById(id);
        if (wh == null) throw new BusinessException("仓库不存在");
        wh.setStatus(wh.getStatus() == 1 ? 0 : 1);
        mapper.updateById(wh);
    }

    private String generateCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "WH-" + today + "-";
        Long count = mapper.selectCount(
                new LambdaQueryWrapper<Warehouse>().likeRight(Warehouse::getWarehouseCode, prefix));
        return String.format("WH-%s-%03d", today, (count != null ? count : 0) + 1);
    }
}