package org.example.graduation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.graduation.dto.ArrivalCreateDTO;
import org.example.graduation.vo.ArrivalVO;

public interface ArrivalService {

    Page<ArrivalVO> list(Integer page, Integer size, String keyword, Long orderId);

    ArrivalVO register(Long orderId, ArrivalCreateDTO dto);
}