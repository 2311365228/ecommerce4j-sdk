package com.ecommerce4j.api.platform.shopee.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Shopee OpenAPI 通用响应字段
 */
@Data
public class ShopeeResponse {

    /**
     * 错误码，成功时为空字符串
     */
    private String error;

    /**
     * 错误描述，成功时为空字符串
     */
    private String message;

    /**
     * Shopee 请求 ID，用于排查平台侧问题
     */
    @JsonProperty("request_id")
    private String requestId;
}
