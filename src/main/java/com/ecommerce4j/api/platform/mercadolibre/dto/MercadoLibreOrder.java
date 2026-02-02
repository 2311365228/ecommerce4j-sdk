package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Mercado Libre 订单数据类。
 * <p>
 * 封装了从 Mercado Libre 开放平台获取的订单详情信息。
 * 该结构主要基于 <code>/orders/{order_id}</code> 接口的响应体。
 * 使用静态内部类来表示嵌套的JSON对象。
 * </p>
 */
@Data
public class MercadoLibreOrder {

    /**
     * 订单的唯一数字标识符。
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 订单的当前状态 (例如: "paid", "cancelled")。
     */
    @JsonProperty("status")
    private String status;

    /**
     * 订单创建时间的ISO 8601格式字符串。
     */
    @JsonProperty("date_created")
    private String dateCreated;

    /**
     * 订单关闭时间的ISO 8601格式字符串 (通常在付款后)。
     */
    @JsonProperty("date_closed")
    private String dateClosed;

    /**
     * 订单中包含的所有商品项目的列表。
     */
    @JsonProperty("order_items")
    private List<OrderItem> orderItems;

    /**
     * 订单的总金额。
     */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /**
     * 货币单位的ID (例如: "MXN", "BRL")。
     */
    @JsonProperty("currency_id")
    private String currencyId;

    /**
     * 包含买家信息的对象。
     */
    @JsonProperty("buyer")
    private Buyer buyer;

    /**
     * 包含卖家信息的对象。
     */
    @JsonProperty("seller")
    private Seller seller;

    /**
     * 与此订单关联的支付信息列表。
     */
    @JsonProperty("payments")
    private List<Payment> payments;

    /**
     * 与此订单关联的物流信息，这是获取履约详情的关键。
     */
    @JsonProperty("shipping")
    private Shipping shipping;

    /**
     * 包裹id，多订单一包裹会返回该值
     */
    @JsonProperty("pack_id")
    private String packId;

    // --- 内部类定义 ---

    /**
     * 代表订单中的一个商品行项目。
     */
    @Data
    public static class OrderItem {
        /**
         * 商品详情。
         */
        @JsonProperty("item")
        private Item item;

        /**
         * 购买的商品数量。
         */
        @JsonProperty("quantity")
        private Integer quantity;

        /**
         * 商品单价。
         */
        @JsonProperty("unit_price")
        private BigDecimal unitPrice;

        /**
         * 此行项目的总金额。
         */
        @JsonProperty("full_unit_price")
        private BigDecimal fullUnitPrice;

        /**
         * 货币单位。
         */
        @JsonProperty("currency_id")
        private String currencyId;

        /**
         * 仓库（如果该属性有值，说明是Mercado Full仓出货）
         */
        @JsonProperty("stock")
        private Object stock;
    }

    /**
     * 代表一个具体的商品刊登信息。
     */
    @Data
    public static class Item {
        /**
         * 商品刊登ID (例如: "MLM123456789")。
         */
        @JsonProperty("id")
        private String id;

        /**
         * 商品标题。
         */
        @JsonProperty("title")
        private String title;

        /**
         * 卖家为商品设置的SKU。
         */
        @JsonProperty("seller_sku")
        private String sellerSku;
    }

    /**
     * 代表买家信息。
     */
    @Data
    public static class Buyer {
        /**
         * 买家的唯一数字ID。
         */
        @JsonProperty("id")
        private Long id;

        /**
         * 买家的昵称。
         */
        @JsonProperty("nickname")
        private String nickname;

        /**
         * 买家的名字。
         */
        @JsonProperty("first_name")
        private String firstName;

        /**
         * 买家的姓氏。
         */
        @JsonProperty("last_name")
        private String lastName;
    }

    /**
     * 代表卖家信息。
     */
    @Data
    public static class Seller {
        /**
         * 卖家的唯一数字ID。
         */
        @JsonProperty("id")
        private Long id;
    }

    /**
     * 代表一笔支付交易。
     */
    @Data
    public static class Payment {
        /**
         * 支付的唯一ID。
         */
        @JsonProperty("id")
        private Long id;

        /**
         * 支付的状态 (例如: "approved")。
         */
        @JsonProperty("status")
        private String status;

        /**
         * 此笔交易的金额。
         */
        @JsonProperty("transaction_amount")
        private BigDecimal transactionAmount;

        /**
         * 货币单位。
         */
        @JsonProperty("currency_id")
        private String currencyId;
    }

    /**
     * 代表订单的物流信息。
     */
    @Data
    public static class Shipping {
        /**
         * 物流货运的唯一ID (shipment_id)，用于所有物流相关操作。
         */
        @JsonProperty("id")
        private String id;

        /**
         * 物流状态 (例如: "handling", "ready_to_ship")。
         */
        @JsonProperty("status")
        private String status;

        /**
         * 收货地址详情。
         */
        @JsonProperty("receiver_address")
        private ReceiverAddress receiverAddress;
    }

    /**
     * 代表收货地址。
     */
    @Data
    public static class ReceiverAddress {
        @JsonProperty("id")
        private Long id;

        /**
         * 街道和门牌号。
         */
        @JsonProperty("address_line")
        private String addressLine;

        /**
         * 邮政编码。
         */
        @JsonProperty("zip_code")
        private String zipCode;

        /**
         * 城市信息。
         */
        @JsonProperty("city")
        private City city;

        /**
         * 州/省份信息。
         */
        @JsonProperty("state")
        private State state;

        /**
         * 国家信息。
         */
        @JsonProperty("country")
        private Country country;

        /**
         * 收件人全名。
         */
        @JsonProperty("receiver_name")
        private String receiverName;

        /**
         * 收件人电话。
         */
        @JsonProperty("receiver_phone")
        private String receiverPhone;
    }

    /**
     * 代表城市。
     */
    @Data
    public static class City {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
    }

    /**
     * 代表州/省份。
     */
    @Data
    public static class State {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
    }

    /**
     * 代表国家。
     */
    @Data
    public static class Country {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
    }
}
