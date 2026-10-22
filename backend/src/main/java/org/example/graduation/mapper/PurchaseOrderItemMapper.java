package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.PurchaseOrderItem;

public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    /** 锁定采购明细，防止并发到货导致超收 */
    @Select("SELECT * FROM purchase_order_item WHERE id = #{id} FOR UPDATE")
    PurchaseOrderItem selectForUpdate(@Param("id") Long id);
}