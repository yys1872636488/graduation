package org.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.graduation.entity.PurchaseOrder;

public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /** 锁定采购订单，串行化同一订单的到货登记 */
    @Select("SELECT * FROM purchase_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PurchaseOrder selectForUpdate(@Param("id") Long id);
}