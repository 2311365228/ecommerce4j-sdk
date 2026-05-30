package com.ecommerce4j.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 平台打包请求
 */
@Data
@Builder
public class FulfillmentPackRequest {

    /**
     * 平台订单ID
     */
    private String orderId;

    /**
     * 平台订单行ID列表
     */
    private List<String> orderLineIds;

    /**
     * 平台履约要求的 delivery_type
     */
    private String deliveryType;

    /**
     * 平台履约要求的发货分配类型
     */
    private String shippingAllocateType;

    /**
     * 平台履约服务商编码
     */
    private String shipmentProviderCode;
}
