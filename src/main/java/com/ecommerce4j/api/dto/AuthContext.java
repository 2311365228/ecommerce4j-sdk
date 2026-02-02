package com.ecommerce4j.api.dto;

import com.ecommerce4j.api.enums.Platform;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 授权上下文
 * <p>
 * 封装授权后获得的所有凭证，作为所有 API 调用的身份认证信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext {

    /**
     * 目标平台
     */
    private Platform platform;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 访问令牌过期时间点
     */
    private Instant accessTokenExpiresAt;

    /**
     * 刷新令牌过期时间点
     */
    private Instant refreshTokenExpiresAt;

    /**
     * 店铺ID (如 TikTok Shop 的 shop_id)
     */
    private String shopId;

    /**
     * 店铺加密凭证
     */
    private String shopCipher;

    /**
     * 卖家ID (如 Mercado Libre 的 seller_id)
     */
    private String sellerId;
}
