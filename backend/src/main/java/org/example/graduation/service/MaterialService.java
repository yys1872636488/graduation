package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.entity.Material;

public interface MaterialService {
    Page<Material> list(Integer page, Integer size, String keyword, Integer status);
    Material create(Material material);
    Material update(Long id, Material material);
    void toggleStatus(Long id);
}