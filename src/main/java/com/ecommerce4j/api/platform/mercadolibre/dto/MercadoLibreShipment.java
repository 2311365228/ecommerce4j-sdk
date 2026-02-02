package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Mercado Libre 货运数据类。
 * <p>
 * 封装了从 Mercado Libre 开放平台获取的货运(Shipment)详情信息。
 * 该结构主要基于 <code>/marketplace/shipments/{shipment_id}</code> 接口并使用 <code>x-format-new: true</code> 请求头获取的响应体。
 * 经过重构以匹配最新的API JSON结构。
 * </p>
 */
@Data
public class MercadoLibreShipment {

    /**
     * 货运的唯一数字标识符 (shipment_id)。
     */
    @JsonProperty("id")
    private String id;

    /**
     * 货运的主状态 (例如: "handling", "ready_to_ship", "shipped", "delivered")。
     */
    @JsonProperty("status")
    private String status;

    /**
     * 货运的子状态，提供比主状态更详细的信息。
     */
    @JsonProperty("substatus")
    private String substatus;

    /**
     * 运单号。
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;

    /**
     * 跟踪方式或承运商名称。
     */
    @JsonProperty("tracking_method")
    private String trackingMethod;

    /**
     * 货运的创建时间，ISO 8601格式。
     */
    @JsonProperty("date_created")
    private String dateCreated;

    /**
     * 最后更新时间，ISO 8601格式。
     */
    @JsonProperty("last_updated")
    private String lastUpdated;

    /**
     * 外部参考号，通常是订单ID。
     */
    @JsonProperty("external_reference")
    private String externalReference;

    /**
     * 声明价值。
     */
    @JsonProperty("declared_value")
    private Double declaredValue;

    /**
     * 物流相关信息，包含模式和类型。
     */
    @JsonProperty("logistic")
    private Logistic logistic;

    /**
     * 目的地信息，包含收件人及收货地址。
     */
    @JsonProperty("destination")
    private Destination destination;

    /**
     * 发货源信息，包含发件人及发货地址。
     */
    @JsonProperty("origin")
    private Origin origin;

    /**
     * 包裹尺寸和重量信息。
     */
    @JsonProperty("dimensions")
    private Dimensions dimensions;

    /**
     * 预计送达时间、成本等相关信息。
     */
    @JsonProperty("lead_time")
    private LeadTime leadTime;


    // --- 内部类定义 ---

    /**
     * 物流信息。
     */
    @Data
    public static class Logistic {
        @JsonProperty("mode")
        private String mode;

        @JsonProperty("type")
        private String type;

        @JsonProperty("direction")
        private String direction;
    }

    /**
     * 目的地信息。
     */
    @Data
    public static class Destination {
        @JsonProperty("receiver_id")
        private Long receiverId;

        @JsonProperty("receiver_name")
        private String receiverName;

        @JsonProperty("receiver_phone")
        private String receiverPhone;

        @JsonProperty("shipping_address")
        private ShippingAddress shippingAddress;
    }

    /**
     * 发货源信息。
     */
    @Data
    public static class Origin {
        @JsonProperty("type")
        private String type;

        @JsonProperty("sender_id")
        private Long senderId;

        @JsonProperty("shipping_address")
        private ShippingAddress shippingAddress;
    }

    /**
     * 统一的地址信息类，可用于发货和收货地址。
     */
    @Data
    public static class ShippingAddress {
        @JsonProperty("address_id")
        private Long addressId;

        @JsonProperty("address_line")
        private String addressLine;

        @JsonProperty("street_name")
        private String streetName;

        @JsonProperty("street_number")
        private String streetNumber;

        @JsonProperty("comment")
        private String comment;

        @JsonProperty("zip_code")
        private String zipCode;

        @JsonProperty("city")
        private AddressComponent city;

        @JsonProperty("state")
        private AddressComponent state;

        @JsonProperty("country")
        private AddressComponent country;

        @JsonProperty("neighborhood")
        private AddressComponent neighborhood;

        @JsonProperty("latitude")
        private Double latitude;

        @JsonProperty("longitude")
        private Double longitude;
    }

    /**
     * 地址组成部分（如城市、州、国家）。
     */
    @Data
    public static class AddressComponent {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }

    /**
     * 包裹尺寸和重量。
     */
    @Data
    public static class Dimensions {
        @JsonProperty("height")
        private Integer height;

        @JsonProperty("width")
        private Integer width;

        @JsonProperty("length")
        private Integer length;

        @JsonProperty("weight")
        private Integer weight;
    }

    /**
     * 交付周期和成本信息。
     */
    @Data
    public static class LeadTime {
        @JsonProperty("cost")
        private Double cost;

        @JsonProperty("cost_type")
        private String costType;

        @JsonProperty("list_cost")
        private Double listCost;

        @JsonProperty("currency_id")
        private String currencyId;

        @JsonProperty("shipping_method")
        private ShippingMethod shippingMethod;

        @JsonProperty("estimated_delivery_time")
        private EstimatedDeliveryTime estimatedDeliveryTime;
    }

    /**
     * 运输方式详情。
     */
    @Data
    public static class ShippingMethod {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("deliver_to")
        private String deliverTo;
    }

    /**
     * 预计送达时间详情。
     */
    @Data
    public static class EstimatedDeliveryTime {
        @JsonProperty("type")
        private String type;

        @JsonProperty("date")
        private String date;

        @JsonProperty("shipping")
        private Integer shipping;

        @JsonProperty("handling")
        private Integer handling;

        @JsonProperty("unit")
        private String unit;
    }
}
