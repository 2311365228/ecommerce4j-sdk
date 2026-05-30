package com.ecommerce4j.api.platform.lazada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Lazada 授权与店铺 DTO
 */
public final class LazadaAuthModels {

    private LazadaAuthModels() {
    }

    @Data
    public static class TokenResponse extends LazadaResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_expires_in")
        private Long refreshExpiresIn;

        @JsonProperty("account_id")
        private String accountId;

        private String account;

        private String country;

        @JsonProperty("account_platform")
        private String accountPlatform;

        @JsonProperty("country_user_info")
        private List<CountryUserInfo> countryUserInfo;

        @JsonProperty("country_user_info_list")
        private List<CountryUserInfo> countryUserInfoList;
    }

    @Data
    public static class CountryUserInfo {

        private String country;

        @JsonProperty("seller_id")
        private String sellerId;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("short_code")
        private String shortCode;
    }

    @Data
    public static class SellerResponse extends LazadaResponse {

        private Seller data;
    }

    @Data
    public static class Seller {

        @JsonProperty("seller_id")
        private String sellerId;

        private String name;

        @JsonProperty("name_company")
        private String companyName;

        @JsonProperty("short_code")
        private String shortCode;

        @JsonProperty("logo_url")
        private String logoUrl;

        private String email;

        private Boolean cb;

        private String location;

        private String status;

        private Boolean verified;

        private Boolean marketplaceEaseMode;
    }
}
