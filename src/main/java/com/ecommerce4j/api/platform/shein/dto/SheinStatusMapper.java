package com.ecommerce4j.api.platform.shein.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * SHEIN 状态映射
 */
public class SheinStatusMapper {

    private static final Map<String, UnifiedOrderStatus> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("WAIT_AUDIT", UnifiedOrderStatus.PENDING_APPROVAL);
        STATUS_MAP.put("PENDING_AUDIT", UnifiedOrderStatus.PENDING_APPROVAL);

        STATUS_MAP.put("WAIT_PAY", UnifiedOrderStatus.PENDING_PAYMENT);
        STATUS_MAP.put("PENDING_PAYMENT", UnifiedOrderStatus.PENDING_PAYMENT);
        STATUS_MAP.put("UNPAID", UnifiedOrderStatus.PENDING_PAYMENT);

        STATUS_MAP.put("PAID", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("WAIT_SHIP", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("TO_BE_SHIPPED", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("PENDING_SHIPMENT", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("READY_TO_SHIP", UnifiedOrderStatus.READY_FOR_FULFILLMENT);

        STATUS_MAP.put("PART_SHIPPED", UnifiedOrderStatus.IN_TRANSIT);
        STATUS_MAP.put("PARTIALLY_SHIPPED", UnifiedOrderStatus.IN_TRANSIT);
        STATUS_MAP.put("SHIPPED", UnifiedOrderStatus.IN_TRANSIT);
        STATUS_MAP.put("IN_TRANSIT", UnifiedOrderStatus.IN_TRANSIT);

        STATUS_MAP.put("DELIVERED", UnifiedOrderStatus.DELIVERED);
        STATUS_MAP.put("RECEIVED", UnifiedOrderStatus.DELIVERED);

        STATUS_MAP.put("COMPLETED", UnifiedOrderStatus.COMPLETED);
        STATUS_MAP.put("FINISHED", UnifiedOrderStatus.COMPLETED);

        STATUS_MAP.put("CANCELLED", UnifiedOrderStatus.CANCELLED);
        STATUS_MAP.put("CANCELED", UnifiedOrderStatus.CANCELLED);
        STATUS_MAP.put("CLOSED", UnifiedOrderStatus.CANCELLED);
        STATUS_MAP.put("VOIDED", UnifiedOrderStatus.CANCELLED);
    }

    private SheinStatusMapper() {
    }

    public static UnifiedOrderStatus toUnifiedStatus(String sheinStatus) {
        if (sheinStatus == null) {
            return UnifiedOrderStatus.UNKNOWN;
        }
        return STATUS_MAP.getOrDefault(sheinStatus.trim().toUpperCase(), UnifiedOrderStatus.UNKNOWN);
    }
}
