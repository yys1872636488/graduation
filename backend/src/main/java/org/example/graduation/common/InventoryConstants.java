package org.example.graduation.common;

/**
 * 库存对象类型、方向与业务类型常量。
 */
public final class InventoryConstants {

    private InventoryConstants() {
    }

    public static final String ITEM_MATERIAL = "MATERIAL";
    public static final String ITEM_FINISHED = "FINISHED";

    public static final String DIRECTION_IN = "IN";
    public static final String DIRECTION_OUT = "OUT";

    public static final String MATERIAL_INBOUND = "MATERIAL_INBOUND";
    public static final String MATERIAL_ISSUE = "MATERIAL_ISSUE";
    public static final String FINISHED_INBOUND = "FINISHED_INBOUND";
    public static final String SALES_OUTBOUND = "SALES_OUTBOUND";
}