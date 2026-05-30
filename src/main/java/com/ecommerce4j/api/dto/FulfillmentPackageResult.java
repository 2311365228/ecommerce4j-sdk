package com.ecommerce4j.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 平台打包结果
 */
@Data
@Builder
public class FulfillmentPackageResult {

    /**
     * 平台订单ID
     */
    private String orderId;

    /**
     * 平台订单行ID列表
     */
    private List<String> orderLineIds;

    /**
     * 平台包裹ID
     */
    private String packageId;

    /**
     * 物流追踪号
     */
    private String trackingNumber;

    /**
     * 平台返回的物流商展示名
     */
    private String shipmentProviderName;

    /**
     * 平台履约服务商编码
     */
    private String shipmentProviderCode;

    /**
     * 平台返回的发货分配类型
     */
    private String shippingAllocateType;

    /**
     * 是否允许重试
     */
    private boolean retryable;

    /**
     * 平台返回的处理结果消息
     */
    private String message;
}
