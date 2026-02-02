package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
/**
 * Mercado Libre 授权令牌数据类。
 * 封装了从 Mercado Libre 开放平台获取的授权信息，包括访问令牌、刷新令牌和过期时间等。
 */
@Data
public class MercadoLibreTokenData {
    /**
     * 访问令牌，用于调用 Mercado Libre API。
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 令牌类型，通常为 "Bearer"。
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 令牌的过期时间，单位为秒。
     */
    @JsonProperty("expires_in")
    private long expiresIn;

    /**
     * 授权范围，指示该令牌被授予的权限。
     */
    private String scope;

    /**
     * 用户的唯一身份标识。
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 刷新令牌，用于在访问令牌过期后获取新的访问令牌。
     */
    @JsonProperty("refresh_token")
    private String refreshToken;
}
