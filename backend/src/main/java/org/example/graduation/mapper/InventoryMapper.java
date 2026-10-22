package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.Inventory;

import java.math.BigDecimal;

public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("SELECT * FROM inventory WHERE item_type = 'MATERIAL' AND batch_id = #{batchId} " +
            "AND available_quantity >= #{quantity} ORDER BY id LIMIT 1 FOR UPDATE")
    Inventory selectMaterialForUpdate(@Param("batchId") Long batchId,
                                      @Param("quantity") BigDecimal quantity);

    @Select("SELECT * FROM inventory WHERE item_type = 'FINISHED' AND batch_id = #{batchId} " +
            "AND available_quantity >= #{quantity} ORDER BY id LIMIT 1 FOR UPDATE")
    Inventory selectFinishedForUpdate(@Param("batchId") Long batchId,
                                      @Param("quantity") BigDecimal quantity);
}