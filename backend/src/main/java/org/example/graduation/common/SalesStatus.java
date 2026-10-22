package org.example.graduation.common;

/**
 * 销售与售后状态常量。
 */
public final class SalesStatus {

    private SalesStatus() {
    }

    // 销售订单
    public static final String ORDER_DRAFT = "DRAFT";
    public static final String ORDER_PENDING_SHIPMENT = "PENDING_SHIPMENT";
    public static final String ORDER_PARTIAL_SHIPPED = "PARTIAL_SHIPPED";
    public static final String ORDER_SHIPPED = "SHIPPED";
    public static final String ORDER_CANCELLED = "CANCELLED";

    // 售后类型
    public static final String TYPE_RETURN = "RETURN";
    public static final String TYPE_EXCHANGE = "EXCHANGE";
    public static final String TYPE_REPAIR = "REPAIR";
    public static final String TYPE_CONSULT = "CONSULT";

    // 售后状态
    public static final String AFTER_PENDING = "PENDING";
    public static final String AFTER_PROCESSING = "PROCESSING";
    public static final String AFTER_PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
    public static final String AFTER_COMPLETED = "COMPLETED";
    public static final String AFTER_CLOSED = "CLOSED";
}