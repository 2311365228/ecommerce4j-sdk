package com.ecommerce4j.api.platform.tiktok.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * TikTokStatusMapper 类，用于将TikTok订单状态映射为统一的内部状态。
 * 这有助于在系统内部标准化不同来源的订单状态，以便于处理和管理。
 */
public class TikTokStatusMapper {

    /**
     * statusMap 是一个静态的 Map，用于存储TikTok原始状态和我们系统内部统一状态的映射关系。
     * 键为TikTok的原始订单状态字符串，值为我们系统内部定义的统一状态枚举。
     */
    private static final Map<String, UnifiedOrderStatus> statusMap = new HashMap<>();

    /**
     * 静态初始化块，在类加载时执行。
     * 在这里我们填充了 statusMap，建立起TikTok状态与统一状态的映射。
     * 这些映射关系是根据特定文档（如参考文档 5.2 节）定义的。
     */
    static {
        statusMap.put("UNPAID", UnifiedOrderStatus.PENDING_PAYMENT);        // 未支付 -> 等待支付
        statusMap.put("ON_HOLD", UnifiedOrderStatus.PENDING_APPROVAL);     // 订单暂停 -> 等待审批
        statusMap.put("AWAITING_SHIPMENT", UnifiedOrderStatus.READY_FOR_FULFILLMENT); // 待发货 -> 准备发货
        statusMap.put("AWAITING_COLLECTION", UnifiedOrderStatus.READY_FOR_FULFILLMENT); // 待揽收 -> 待揽收
        statusMap.put("IN_TRANSIT", UnifiedOrderStatus.IN_TRANSIT);         // 运输中 -> 运输中
        statusMap.put("DELIVERED", UnifiedOrderStatus.DELIVERED);           // 已妥投 -> 已妥投
        statusMap.put("COMPLETED", UnifiedOrderStatus.COMPLETED);           // 已完成 -> 已完成
        statusMap.put("CANCELLED", UnifiedOrderStatus.CANCELLED); // 已取消 -> 已取消
    }

    /**
     * 将TikTok的订单状态转换为我们系统内部的统一状态。
     * 如果传入的TikTok状态在映射中不存在，则返回"UNKNOWN"（未知）。
     *
     * @param tiktokStatus TikTok的原始订单状态
     * @return 映射后的统一订单状态，如果找不到则返回"UNKNOWN"
     */
    public static UnifiedOrderStatus toUnifiedStatus(String tiktokStatus) {
        // 使用 getOrDefault 方法可以优雅地处理找不到键的情况，避免了空指针异常。
        return statusMap.getOrDefault(tiktokStatus, UnifiedOrderStatus.UNKNOWN);
    }
}
