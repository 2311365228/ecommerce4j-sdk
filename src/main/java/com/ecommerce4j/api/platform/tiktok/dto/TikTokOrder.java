package com.ecommerce4j.api.platform.tiktok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
/**
 * TikTok订单数据类。
 * 封装了从TikTok开放平台获取的订单详情信息。
 */
@Data
public class TikTokOrder {

    /**
     * 订单ID。
     */
    @JsonProperty("id")
    private String id;
    /**
     * 订单状态。
     */
    @JsonProperty("status")
    private String status;
    /**
     * 订单创建时间，Unix时间戳（秒）。
     */
    @JsonProperty("create_time")
    private long createTime;
    /**
     * 订单最后更新时间，Unix时间戳（秒）。
     */
    @JsonProperty("update_time")
    private long updateTime;
    /**
     * 买家留言。
     */
    @JsonProperty("buyer_message")
    private String buyerMessage;
    /**
     * 物流服务商名称。
     */
    @JsonProperty("shipping_provider")
    private String shippingProvider;
    /**
     * 订单追踪号码。
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;
    /**
     * 收件人地址信息。
     */
    @JsonProperty("recipient_address")
    private TikTokRecipientAddress recipientAddress;
    /**
     * 支付信息。
     */
    @JsonProperty("payment")
    private PaymentInfo payment;
    /**
     * 订单中的商品行项目列表。
     */
    @JsonProperty("line_items")
    private List<LineItem> lineItems;
    /**
     * 订单中的包裹信息列表。
     */
    @JsonProperty("packages")
    private List<Package> packages;

    /**
     * 支付信息内部类。
     * 描述了订单的支付详情。
     */
    @Data
    public static class PaymentInfo {
        /**
         * 币种。
         */
        @JsonProperty("currency")
        private String currency;
        /**
         * 总金额。
         */
        @JsonProperty("total_amount")
        private String totalAmount;
    }

    /**
     * 订单商品行项目内部类。
     * 描述了订单中每个商品的详细信息。
     */
    @Data
    public static class LineItem {
        /**
         * 商品行项目ID。
         */
        @JsonProperty("id")
        private String id;

        /**
         * 货币单位
         */
        @JsonProperty("currency")
        private String currency;

        @JsonProperty("display_status")
        private String displayStatus;
        /**
         * 商品SKU名称。
         */
        @JsonProperty("sku_name")
        private String skuName;
        /**
         * 商品名称。
         */
        @JsonProperty("product_name")
        private String productName;
        /**
         * 卖家SKU。
         */
        @JsonProperty("seller_sku")
        private String sellerSku;
        /**
         * 商品原始价格。
         */
        @JsonProperty("original_price")
        private String originalPrice;
        /**
         * 购买数量。
         */
        @JsonProperty("quantity")
        private Integer quantity;

        /**
         * sku图片地址
         */
        @JsonProperty("sku_image")
        private String skuImage;

        /**
         * 是否是礼物
         */
        @JsonProperty("is_gift")
        private Boolean isGift;

        /**
         * 销售价格
         */
        @JsonProperty("sale_price")
        private String salePrice;

        /**
         * 卖家折扣
         */
        @JsonProperty("seller_discount")
        private String sellerDiscount;

        /**
         * 平台折扣
         */
        @JsonProperty("platform_discount")
        private String platformDiscount;

        /**
         * SKU ID
         */
        @JsonProperty("sku_id")
        private String skuId;

        /**
         * 商品 ID
         */
        @JsonProperty("product_id")
        private String productId;

        /**
         * 运输服务商 ID
         */
        @JsonProperty("shipping_provider_id")
        private String shippingProviderId;

        /**
         * 运输服务商名称
         */
        @JsonProperty("shipping_provider_name")
        private String shippingProviderName;

        /**
         * 包裹 ID
         */
        @JsonProperty("package_id")
        private String packageId;

        /**
         * 包裹状态
         */
        @JsonProperty("package_status")
        private String packageStatus;

        /**
         * 跟踪号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;

        /**
         * SKU类型
         */
        @JsonProperty("sku_type")
        private String skuType;

        /**
         * rts时间（发货时间戳）
         */
        @JsonProperty("rts_time")
        private Long rtsTime;
    }

    /**
     * 收件人地址信息内部类。
     * 描述了订单的收件人地址详情。
     */
    @Data
    public static class TikTokRecipientAddress {
        /**
         * 完整地址。
         */
        @JsonProperty("full_address")
        private String fullAddress;
        /**
         * 收件人姓名。
         */
        @JsonProperty("name")
        private String name;
        /**
         * 联系电话。
         */
        @JsonProperty("phone")
        private String phone;
        /**
         * 邮政编码。
         */
        @JsonProperty("postal_code")
        private String postalCode;
        // ... 其他地址字段 city, state, country ...
    }

    /**
     * 包裹信息内部类。
     * 描述了订单的包裹详情。
     */
    @Data
    public static class Package {
        /**
         * 包裹ID。
         */
        @JsonProperty("id")
        private String id;
        /**
         * 物流类型：TIKTOK_SHIPPING或SELLER_SHIPPING。
         */
        @JsonProperty("shipping_type")
        private String shippingType;
    }
}
