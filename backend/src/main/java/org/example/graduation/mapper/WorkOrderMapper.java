package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.WorkOrder;

public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    @Select("SELECT * FROM work_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    WorkOrder selectForUpdate(@Param("id") Long id);
}