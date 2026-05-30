package com.ecommerce4j.api.platform.lazada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Lazada 订单 DTO
 */
public final class LazadaOrderModels {

    private LazadaOrderModels() {
    }

    @Data
    public static class OrdersResponse extends LazadaResponse {

        private OrdersData data;
    }

    @Data
    public static class OrdersData {

        private Integer count;

        @JsonProperty("countTotal")
        private Integer countTotal;

        private List<Order> orders;
    }

    @Data
    public static class OrderResponse extends LazadaResponse {

        private Order data;
    }

    @Data
    public static class OrderItemsResponse extends LazadaResponse {

        private List<OrderItem> data;
    }

    @Data
    public static class MultipleOrderItemsResponse extends LazadaResponse {

        private List<OrderItemsGroup> data;
    }

    @Data
    public static class OrderItemsGroup {

        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_number")
        private String orderNumber;

        @JsonProperty("order_items")
        private List<OrderItem> orderItems;
    }

    @Data
    public static class Order {

        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_number")
        private String orderNumber;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        private List<String> statuses;

        @JsonProperty("items_count")
        private Integer itemsCount;

        private String price;

        @JsonProperty("shipping_fee")
        private String shippingFee;

        private String voucher;

        @JsonProperty("voucher_platform")
        private String voucherPlatform;

        @JsonProperty("voucher_seller")
        private String voucherSeller;

        @JsonProperty("payment_method")
        private String paymentMethod;

        @JsonProperty("promised_shipping_times")
        private String promisedShippingTimes;

        @JsonProperty("warehouse_code")
        private String warehouseCode;

        @JsonProperty("buyer_note")
        private String buyerNote;

        @JsonProperty("customer_first_name")
        private String customerFirstName;

        @JsonProperty("customer_last_name")
        private String customerLastName;

        @JsonProperty("need_cancel_confirm")
        private Boolean needCancelConfirm;

        @JsonProperty("is_cancel_pending")
        private Boolean cancelPending;

        @JsonProperty("address_shipping")
        private Address addressShipping;

        @JsonProperty("address_billing")
        private Address addressBilling;

        @JsonProperty("recipient_info")
        private RecipientInfo recipientInfo;
    }

    @Data
    public static class Address {

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String phone;

        private String phone2;

        private String address1;

        private String address2;

        private String address3;

        private String address4;

        private String address5;

        @JsonProperty("addressDistrict")
        private String addressDistrict;

        @JsonProperty("addressDsitrict")
        private String addressDsitrict;

        private String city;

        @JsonProperty("post_code")
        private String postCode;

        private String country;
    }

    @Data
    public static class RecipientInfo {

        @JsonProperty("passport_no")
        private String passportNo;

        @JsonProperty("identify_no")
        private String identifyNo;

        @JsonProperty("detail_address")
        private String detailAddress;
    }

    @Data
    public static class OrderItem {

        @JsonProperty("order_item_id")
        private String orderItemId;

        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("shop_sku")
        private String shopSku;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("sku_id")
        private String skuId;

        private String name;

        private String status;

        @JsonProperty("package_id")
        private String packageId;

        @JsonProperty("shipment_provider")
        private String shipmentProvider;

        @JsonProperty("tracking_code")
        private String trackingCode;

        @JsonProperty("shipping_provider_type")
        private String shippingProviderType;

        @JsonProperty("shipping_type")
        private String shippingType;

        @JsonProperty("item_price")
        private String itemPrice;

        @JsonProperty("paid_price")
        private String paidPrice;

        @JsonProperty("tax_amount")
        private String taxAmount;

        @JsonProperty("shipping_amount")
        private String shippingAmount;

        @JsonProperty("shipping_service_cost")
        private String shippingServiceCost;

        @JsonProperty("voucher_amount")
        private String voucherAmount;

        @JsonProperty("voucher_platform")
        private String voucherPlatform;

        @JsonProperty("voucher_seller")
        private String voucherSeller;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        @JsonProperty("promised_shipping_time")
        private String promisedShippingTime;

        @JsonProperty("sla_time_stamp")
        private String slaTimeStamp;

        @JsonProperty("order_type")
        private String orderType;

        @JsonProperty("delivery_option_sof")
        private String deliveryOptionSof;

        @JsonProperty("is_fbl")
        private String fbl;

        @JsonProperty("biz_group")
        private Long bizGroup;

        @JsonProperty("product_main_image")
        private String productMainImage;

        private String currency;

        @JsonProperty("cancel_return_initiator")
        private String cancelReturnInitiator;

        @JsonProperty("pick_up_store_info")
        private PickUpStoreInfo pickUpStoreInfo;
    }

    @Data
    public static class PickUpStoreInfo {

        @JsonProperty("pick_up_store_name")
        private String pickUpStoreName;

        @JsonProperty("pick_up_store_address")
        private String pickUpStoreAddress;

        @JsonProperty("pick_up_store_code")
        private String pickUpStoreCode;

        @JsonProperty("pick_up_store_open_hour")
        private List<String> pickUpStoreOpenHour;
    }
}
