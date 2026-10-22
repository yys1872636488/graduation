package org.example.graduation.common;

/**
 * 采购与批次状态常量。
 */
public final class ProcurementStatus {

    private ProcurementStatus() {
    }

    // 采购订单
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String PARTIAL_RECEIVED = "PARTIAL_RECEIVED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    // 原材料批次检验状态
    public static final String PENDING_INSPECTION = "PENDING_INSPECTION";
    public static final String INSPECTING = "INSPECTING";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String QUALIFIED = "QUALIFIED";
    public static final String UNQUALIFIED = "UNQUALIFIED";
    public static final String ON_HOLD = "ON_HOLD";
    public static final String STOCKED = "STOCKED";
    public static final String DEPLETED = "DEPLETED";

    // 机器视觉检测结果状态
    public static final String DETECT_SUCCESS = "SUCCESS";
    public static final String DETECT_MODEL_NOT_CONFIGURED = "MODEL_NOT_CONFIGURED";
    public static final String DETECT_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String DETECT_FAILED = "FAILED";
}