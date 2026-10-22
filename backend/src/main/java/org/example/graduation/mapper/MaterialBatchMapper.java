package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.MaterialBatch;

public interface MaterialBatchMapper extends BaseMapper<MaterialBatch> {

    @Select("SELECT * FROM material_batch WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    MaterialBatch selectForUpdate(@Param("id") Long id);
}