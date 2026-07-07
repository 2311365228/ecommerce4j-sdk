package com.ecommerce4j.api.platform.shopee.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Shopee 订单状态映射器
 */
public final class ShopeeStatusMapper {

    private static final Map<String, UnifiedOrderStatus> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("UNPAID", UnifiedOrderStatus.PENDING_PAYMENT);

        STATUS_MAP.put("PENDING", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("READY_TO_SHIP", UnifiedOrderStatus.READY_FOR_FULFILLMENT);
        STATUS_MAP.put("PROCESSED", UnifiedOrderStatus.READY_FOR_FULFILLMENT);

        STATUS_MAP.put("SHIPPED", UnifiedOrderStatus.IN_TRANSIT);

        STATUS_MAP.put("COMPLETED", UnifiedOrderStatus.COMPLETED);

        STATUS_MAP.put("IN_CANCEL", UnifiedOrderStatus.CANCELLED);
        STATUS_MAP.put("CANCELLED", UnifiedOrderStatus.CANCELLED);
    }

    private ShopeeStatusMapper() {
    }

    public static UnifiedOrderStatus toUnifiedStatus(String shopeeStatus) {
        if (shopeeStatus == null) {
            return UnifiedOrderStatus.UNKNOWN;
        }
        return STATUS_MAP.getOrDefault(shopeeStatus.trim().toUpperCase(), UnifiedOrderStatus.UNKNOWN);
    }
}
