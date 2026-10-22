package org.example.graduation.common;

/**
 * 生产、成品质检与成品批次状态常量。
 */
public final class ProductionStatus {

    private ProductionStatus() {
    }

    // 生产计划
    public static final String PLAN_DRAFT = "DRAFT";
    public static final String PLAN_RELEASED = "RELEASED";
    public static final String PLAN_IN_PROGRESS = "IN_PROGRESS";
    public static final String PLAN_COMPLETED = "COMPLETED";
    public static final String PLAN_CANCELLED = "CANCELLED";

    // 生产工单
    public static final String WO_PENDING = "PENDING";
    public static final String WO_IN_PROGRESS = "IN_PROGRESS";
    public static final String WO_PENDING_QC = "PENDING_QC";
    public static final String WO_REWORKING = "REWORKING";
    public static final String WO_COMPLETED = "COMPLETED";
    public static final String WO_CANCELLED = "CANCELLED";

    // 成品质检结果
    public static final String RESULT_QUALIFIED = "QUALIFIED";
    public static final String RESULT_PARTIAL = "PARTIAL";
    public static final String RESULT_UNQUALIFIED = "UNQUALIFIED";

    // 不合格处理
    public static final String DISPOSITION_REWORK = "REWORK";
    public static final String DISPOSITION_RECHECK = "RECHECK";
    public static final String DISPOSITION_SCRAP = "SCRAP";

    // 成品批次
    public static final String BATCH_PENDING_INBOUND = "PENDING_INBOUND";
    public static final String BATCH_AVAILABLE = "AVAILABLE";
    public static final String BATCH_DEPLETED = "DEPLETED";
}