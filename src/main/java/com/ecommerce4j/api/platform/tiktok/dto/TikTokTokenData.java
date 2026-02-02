package com.ecommerce4j.api.platform.tiktok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * TikTok授权令牌数据类。
 * 封装了从TikTok开放平台获取的授权信息，包括访问令牌、刷新令牌和卖家信息等。
 */
@Data
public class TikTokTokenData {

    /**
     * 访问令牌，用于调用TikTok API。
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 访问令牌的过期时间，单位为秒。
     */
    @JsonProperty("access_token_expire_in")
    private long accessTokenExpireIn;

    /**
     * 刷新令牌，用于在访问令牌过期后获取新的访问令牌。
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 刷新令牌的过期时间，单位为秒。
     */
    @JsonProperty("refresh_token_expire_in")
    private long refreshTokenExpireIn;

    /**
     * 用户的唯一身份标识。
     */
    @JsonProperty("open_id")
    private String openId;

    /**
     * 卖家名称。
     */
    @JsonProperty("seller_name")
    private String sellerName;

    /**
     * 卖家基础区域。
     */
    @JsonProperty("seller_base_region")
    private String sellerBaseRegion;
}
