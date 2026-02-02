package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * MercadoLibreStatusMapper 类，用于将 Mercado Libre 的订单或货运状态映射为统一的内部状态。
 * 这有助于在系统内部标准化不同来源的订单状态，以便于处理和管理。
 */
public class MercadoLibreStatusMapper {

    /**
     * statusMap 是一个静态的 Map，用于存储 Mercado Libre 原始状态和我们系统内部统一状态的映射关系。
     * 键为 Mercado Libre 的原始状态字符串，值为我们系统内部定义的统一状态枚举。
     */
    private static final Map<String, UnifiedOrderStatus> statusMap = new HashMap<>();

    /**
     * 静态初始化块，在类加载时执行。
     * 在这里，我们填充了 statusMap，建立起 Mercado Libre 状态与统一状态的映射。
     * 这些映射关系主要参考了 Mercado Libre 的货运（shipment）状态，因为它更准确地反映了订单的履约阶段。
     */
    static {
        statusMap.put("payment_required", UnifiedOrderStatus.PENDING_PAYMENT);       // 需支付 -> 等待支付
        statusMap.put("payment_in_process", UnifiedOrderStatus.PENDING_PAYMENT);     // 支付中 -> 等待支付
        statusMap.put("paid", UnifiedOrderStatus.READY_FOR_FULFILLMENT);             // 已支付 -> 准备发货 (在订单状态中，'paid'通常对应货运状态的'handling'或'ready_to_ship')

        statusMap.put("handling", UnifiedOrderStatus.READY_FOR_FULFILLMENT);         // 处理中 -> 待揽收
        statusMap.put("ready_to_ship", UnifiedOrderStatus.READY_FOR_FULFILLMENT);    // 准备发货 -> 待揽收
        statusMap.put("shipped", UnifiedOrderStatus.IN_TRANSIT);                     // 已发货 -> 运输中
        statusMap.put("delivered", UnifiedOrderStatus.DELIVERED);                    // 已妥投 -> 已妥投
        statusMap.put("not_delivered", UnifiedOrderStatus.IN_TRANSIT);               // 未妥投 -> 运输中 (可被视为运输中有问题)
        statusMap.put("cancelled", UnifiedOrderStatus.CANCELLED);                    // 已取消 -> 已取消
    }

    /**
     * 将 Mercado Libre 的状态转换为我们系统内部的统一状态。
     * 如果传入的状态在映射中不存在，则返回"UNKNOWN"（未知）。
     *
     * @param mercadoLibreStatus Mercado Libre 的原始状态
     * @return 映射后的统一订单状态，如果找不到则返回"UNKNOWN"
     */
    public static UnifiedOrderStatus toUnifiedStatus(String mercadoLibreStatus) {
        // 使用 getOrDefault 方法可以优雅地处理找不到键的情况，避免了空指针异常。
        return statusMap.getOrDefault(mercadoLibreStatus, UnifiedOrderStatus.UNKNOWN);
    }
}
