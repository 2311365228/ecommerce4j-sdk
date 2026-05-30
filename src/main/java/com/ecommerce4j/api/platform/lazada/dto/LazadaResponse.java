package com.ecommerce4j.api.platform.lazada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Lazada Open Platform 通用响应头
 */
@Data
public class LazadaResponse {

    private String code;

    private String type;

    private String message;

    @JsonProperty("request_id")
    private String requestId;
}
