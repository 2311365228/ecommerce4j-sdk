package com.ecommerce4j.api.dto;

import com.ecommerce4j.api.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedShopInfo {

    /**
     * 平台
     */
    private Platform platform;

    /**
     * 账号昵称
     */
    private String userNickName;
    /**
     * 店铺id
     */
    private String shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 国家id
     */
    private String countryId;

    /**
     * 店铺的加密令牌
     */
    private String cipher;

    /**
     * 店铺编码
     */
    private String shopCode;

    /**
     * 卖家类型
     */
    private String sellerType;
}
