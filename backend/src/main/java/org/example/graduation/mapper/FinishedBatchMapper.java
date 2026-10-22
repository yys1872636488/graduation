package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.FinishedBatch;

public interface FinishedBatchMapper extends BaseMapper<FinishedBatch> {

    @Select("SELECT * FROM finished_batch WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    FinishedBatch selectForUpdate(@Param("id") Long id);
}