package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MercadoLibreShipmentHistoryResponse {

    /**
     * 包含所有关键物流节点时间的集合。
     */
    @JsonProperty("date_history")
    private DateHistory dateHistory;

    /**
     * 物流状态的子状态，提供更详细的状态信息。
     */
    @JsonProperty("substatus")
    private String substatus;

    /**
     * 创建此物流记录的角色 (例如: "receiver", "sender")。
     */
    @JsonProperty("created_by")
    private String createdBy;

    /**
     * 物流模式，例如 "me2" 代表 Mercado Envíos 2。
     */
    @JsonProperty("mode")
    private String mode;

    /**
     * 退货时的物流跟踪号。
     */
    @JsonProperty("return_tracking_number")
    private String returnTrackingNumber;

    /**
     * 物流服务ID。
     */
    @JsonProperty("service_id")
    private Long serviceId;

    /**
     * 站点ID (例如: "MLM" 代表墨西哥站)。
     */
    @JsonProperty("site_id")
    private String siteId;

    /**
     * 承运商信息，通常在有外部承运商时才会有数据。
     */
    @JsonProperty("carrier_info")
    private Object carrierInfo;

    /**
     * 主要的物流跟踪号。
     */
    @JsonProperty("tracking_number")
    private String trackingNumber;

    /**
     * 退货物流的跟踪URL。
     */
    @JsonProperty("return_tracking_url")
    private String returnTrackingUrl;

    /**
     * 物流(Shipment)的唯一ID。
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 具体的物流跟踪方式 (例如: "MEL Distribution")。
     */
    @JsonProperty("tracking_method")
    private String trackingMethod;

    /**
     * 与此物流关联的订单ID。
     */
    @JsonProperty("order_id")
    private Long orderId;

    /**
     * 物流跟踪URL。
     */
    @JsonProperty("tracking_url")
    private String trackingUrl;

    /**
     * 当前最终的物流状态 (例如: "delivered", "shipped")。
     */
    @JsonProperty("status")
    private String status;

    /**
     * 存储关键物流事件日期的内部类。
     */
    @Data
    public static class DateHistory {

        /**
         * 实际发货时间。
         */
        @JsonProperty("date_shipped")
        private String dateShipped;

        /**
         * 包裹被退回的时间。
         */
        @JsonProperty("date_returned")
        private String dateReturned;

        /**
         * 包裹成功送达的时间。
         */
        @JsonProperty("date_delivered")
        private String dateDelivered;

        /**
         * 包裹派送失败的时间。
         */
        @JsonProperty("date_not_delivered")
        private String dateNotDelivered;

        /**
         * 物流记录创建时间。
         */
        @JsonProperty("date_created")
        private String dateCreated;

        /**
         * 物流被取消的时间。
         */
        @JsonProperty("date_cancelled")
        private String dateCancelled;

        /**
         * 仓库开始处理包裹的时间。
         */
        @JsonProperty("date_handling")
        private String dateHandling;

        /**
         * 卖家第一次打印运单标签的时间。
         */
        @JsonProperty("date_first_printed")
        private String dateFirstPrinted;

        /**
         * 包裹准备就绪，可以发货的时间。
         */
        @JsonProperty("date_ready_to_ship")
        private String dateReadyToShip;

        /**
         * 预计的送达时间。
         */
        @JsonProperty("date_delivered_estimated")
        private String dateDeliveredEstimated;
    }
}
