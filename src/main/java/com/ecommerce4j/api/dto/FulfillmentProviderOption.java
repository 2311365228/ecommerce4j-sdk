package com.ecommerce4j.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 平台返回的可用履约服务商选项
 */
@Data
@Builder
public class FulfillmentProviderOption {

    /**
     * 平台履约服务商编码
     */
    private String shipmentProviderCode;

    /**
     * 平台履约服务商名称
     */
    private String shipmentProviderName;

    /**
     * 平台返回的发货分配类型
     */
    private String shippingAllocateType;

    /**
     * 是否由平台默认分配，无需商家选择
     */
    private boolean platformDefault;
}
