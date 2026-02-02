package com.ecommerce4j.api.platform.tiktok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @description 店铺信息数据传输对象
 */
@Data
public class TikTokShopInfo {

    /**
     * 店铺列表
     */
    @JsonProperty("shops")
    private List<Shop> shops;

    @Data
    public static class Shop {
        /**
         * 店铺ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 店铺名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 所在区域/国家代码
         */
        @JsonProperty("region")
        private String region;

        /**
         * 卖家类型 (例如: CROSS_BORDER - 跨境)
         */
        @JsonProperty("seller_type")
        private String sellerType;

        /**
         * 加密串或令牌
         */
        @JsonProperty("cipher")
        private String cipher;

        /**
         * 店铺代码
         */
        @JsonProperty("code")
        private String code;
    }
}
