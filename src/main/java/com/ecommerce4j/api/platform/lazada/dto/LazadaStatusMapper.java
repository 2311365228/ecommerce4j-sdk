package com.ecommerce4j.api.platform.lazada.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Lazada 订单状态映射
 */
public class LazadaStatusMapper {

    private static final Map<String, UnifiedOrderStatus> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("UNPAID", UnifiedOrderStatus.PENDING_PAYMENT);

        STATUS_MAP.put("PENDING", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("REPACKED", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("PACKED", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("READY_TO_SHIP_PENDING", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("READY_TO_SHIP", UnifiedOrderStatus.READY_FOR_FULFILLMENT);

        STATUS_MAP.put("SHIPPING", UnifiedOrderStatus.IN_TRANSIT);
        STATUS_MAP.put("SHIPPED", UnifiedOrderStatus.IN_TRANSIT);

        STATUS_MAP.put("DELIVERED", UnifiedOrderStatus.DELIVERED);

        STATUS_MAP.put("CANCELED", UnifiedOrderStatus.CANCELLED);
    }

    private LazadaStatusMapper() {
    }

    public static UnifiedOrderStatus toUnifiedStatus(String lazadaStatus) {
        if (lazadaStatus == null) {
            return UnifiedOrderStatus.UNKNOWN;
        }
        return STATUS_MAP.getOrDefault(lazadaStatus.trim().toUpperCase(), UnifiedOrderStatus.UNKNOWN);
    }
}
