package com.ecommerce4j.api.platform.lazada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Lazada 履约 DTO
 */
public final class LazadaFulfillmentModels {

    private LazadaFulfillmentModels() {
    }

    @Data
    public static class ShipmentProvidersRequest {

        @JsonProperty("orders")
        private List<ShipmentProviderOrder> orders;
    }

    @Data
    public static class ShipmentProviderOrder {

        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_item_ids")
        private List<String> orderItemIds;
    }

    @Data
    public static class ShipmentProvidersResponse extends LazadaResponse {

        private OperationResult<ShipmentProvidersData> result;
    }

    @Data
    public static class ShipmentProvidersData {

        @JsonProperty("platform_default")
        private Integer platformDefault;

        @JsonProperty("shipment_providers")
        private List<ShipmentProvider> shipmentProviders;

        @JsonProperty("shipping_allocate_type")
        private String shippingAllocateType;
    }

    @Data
    public static class ShipmentProvider {

        private String name;

        @JsonProperty("provider_code")
        private String providerCode;
    }

    @Data
    public static class PackRequest {

        @JsonProperty("pack_order_list")
        private List<PackOrder> packOrderList;

        @JsonProperty("delivery_type")
        private String deliveryType;

        @JsonProperty("shipment_provider_code")
        private String shipmentProviderCode;

        @JsonProperty("shipping_allocate_type")
        private String shippingAllocateType;
    }

    @Data
    public static class PackOrder {

        @JsonProperty("order_item_list")
        private List<String> orderItemList;

        @JsonProperty("order_id")
        private String orderId;
    }

    @Data
    public static class PackResponse extends LazadaResponse {

        private OperationResult<PackData> result;
    }

    @Data
    public static class PackData {

        @JsonProperty("pack_order_list")
        private List<PackOrderResult> packOrderList;
    }

    @Data
    public static class PackOrderResult {

        @JsonProperty("order_item_list")
        private List<PackItemResult> orderItemList;

        @JsonProperty("order_id")
        private String orderId;
    }

    @Data
    public static class PackItemResult {

        @JsonProperty("order_item_id")
        private String orderItemId;

        private String msg;

        @JsonProperty("item_err_code")
        private String itemErrCode;

        @JsonProperty("tracking_number")
        private String trackingNumber;

        @JsonProperty("shipment_provider")
        private String shipmentProvider;

        @JsonProperty("package_id")
        private String packageId;

        private Boolean retry;
    }

    @Data
    public static class PackageDocumentRequest {

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("packages")
        private List<PackageRef> packages;

        @JsonProperty("print_item_list")
        private Boolean printItemList;
    }

    @Data
    public static class PackageRef {

        @JsonProperty("package_id")
        private String packageId;
    }

    @Data
    public static class PackageDocumentResponse extends LazadaResponse {

        private OperationResult<PackageDocumentData> result;
    }

    @Data
    public static class PackageDocumentData {

        private String file;

        @JsonProperty("pdf_url")
        private String pdfUrl;

        @JsonProperty("doc_type")
        private String docType;
    }

    @Data
    public static class ReadyToShipRequest {

        private List<PackageRef> packages;
    }

    @Data
    public static class ReadyToShipResponse extends LazadaResponse {

        private OperationResult<ReadyToShipData> result;
    }

    @Data
    public static class ReadyToShipData {

        private List<ReadyToShipPackageResult> packages;
    }

    @Data
    public static class ReadyToShipPackageResult {

        private String msg;

        @JsonProperty("item_err_code")
        private String itemErrCode;

        @JsonProperty("package_id")
        private String packageId;

        private String retry;
    }

    @Data
    public static class OperationResult<T> {

        private T data;

        private Boolean success;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("error_msg")
        private String errorMsg;
    }
}
