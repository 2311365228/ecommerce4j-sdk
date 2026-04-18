package com.ecommerce4j.api.platform.shein.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * SHEIN 授权结果中的核心凭证字段
 */
@Data
public class SheinCredentialData {

    @JsonAlias({"access_token", "token"})
    private String accessToken;

    @JsonAlias({"refresh_token"})
    private String refreshToken;

    @JsonAlias({"access_token_expire_in", "expireIn", "expiresIn"})
    private Long accessTokenExpireIn;

    @JsonAlias({"refresh_token_expire_in", "refreshExpiresIn", "refreshExpireIn"})
    private Long refreshTokenExpireIn;

    @JsonAlias({"open_key_id", "openkeyId"})
    private String openKeyId;

    @JsonAlias({"secret_key"})
    private String secretKey;

    @JsonAlias({"merchant_id", "sellerId", "supplierCode"})
    private String merchantId;

    @JsonAlias({"merchant_name", "sellerName", "storeName"})
    private String merchantName;

    @JsonAlias({"shop_id", "supplierCode"})
    private String shopId;

    @JsonAlias({"shop_name", "storeName", "merchantName"})
    private String shopName;
}
