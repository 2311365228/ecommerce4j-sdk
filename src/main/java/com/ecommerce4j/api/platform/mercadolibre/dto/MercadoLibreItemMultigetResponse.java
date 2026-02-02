package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用于解析 Mercado Libre /items?ids=... (Multiget) 接口响应的包装类。
 * API响应是一个列表，列表中的每个元素都包含 code 和 body。
 */
@Data
public class MercadoLibreItemMultigetResponse {

    @JsonProperty("code")
    private int code;

    @JsonProperty("body")
    private MercadoLibreItem body;
}
