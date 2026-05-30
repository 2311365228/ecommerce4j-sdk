package com.ecommerce4j.api.platform.lazada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Lazada 物流轨迹 DTO
 */
public final class LazadaTraceModels {

    private LazadaTraceModels() {
    }

    @Data
    public static class OrderTraceResponse extends LazadaResponse {

        private OrderTraceResult result;
    }

    @Data
    public static class OrderTraceResult {

        @JsonProperty("error_code")
        private ErrorCode errorCode;

        private Boolean repeated;

        private Boolean retry;

        @JsonProperty("not_success")
        private Boolean notSuccess;

        private Boolean success;

        private List<TraceModule> module;
    }

    @Data
    public static class ErrorCode {

        @JsonProperty("displayMessage")
        private String displayMessage;
    }

    @Data
    public static class TraceModule {

        @JsonProperty("warehouse_detail_info")
        private String warehouseDetailInfo;

        @JsonProperty("ofc_order_id")
        private String ofcOrderId;

        @JsonProperty("package_detail_info_list")
        private List<PackageDetail> packageDetailInfoList;
    }

    @Data
    public static class PackageDetail {

        @JsonProperty("tracking_number")
        private String trackingNumber;

        @JsonProperty("ofc_package_id")
        private String ofcPackageId;

        @JsonProperty("order_line_info_list")
        private String orderLineInfoList;

        @JsonProperty("logistic_detail_info_list")
        private List<TraceEvent> logisticDetailInfoList;
    }

    @Data
    public static class TraceEvent {

        private String title;

        private String description;

        @JsonProperty("event_time")
        private Long eventTime;

        @JsonProperty("event_date")
        private String eventDate;

        @JsonProperty("package_location_name")
        private String packageLocationName;

        @JsonProperty("status_code")
        private String statusCode;

        @JsonProperty("detail_type")
        private String detailType;
    }
}
