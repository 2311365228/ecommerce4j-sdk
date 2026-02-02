package com.ecommerce4j.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TrackingInfo {
    /**
     * 物流运单号
     */
    private String trackingNumber;
    /**
     * 物流服务商在平台侧的ID
     */
    private String shippingProviderId;
    /**
     * [适用于拆单发货] 此包裹包含的订单行项目ID列表
     */
    private List<String> orderLineItemIds;
}
