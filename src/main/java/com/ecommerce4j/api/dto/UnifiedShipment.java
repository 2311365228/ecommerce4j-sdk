package com.ecommerce4j.api.dto;

import lombok.Data;
import java.util.List;

/**
 * 统一货运数据模型
 */
@Data
public class UnifiedShipment {
    /**
     * 平台原始货运/包裹ID
     */
    private String shipmentId;
    /**
     * 映射后的统一货运状态
     */
    private String unifiedStatus;
    /**
     * 平台原始货运状态
     */
    private String originalStatus;
    /**
     * 物流运单号
     */
    private String trackingNumber;
    /**
     * 物流承运商名称
     */
    private String carrier;
    /**
     * 完整的收货地址信息
     */
    private UnifiedAddress shippingAddress;
    /**
     * 物流轨迹事件列表
     */
    private List<UnifiedTrackingEvent> trackingEvents;
}
