package com.ecommerce4j.api.platform.tiktok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于解析获取面单链接API响应的DTO
 */
@NoArgsConstructor
@Data
public class TikTokShippingDocumentResponse {

    @JsonProperty("doc_url")
    private String docUrl;

    @JsonProperty("tracking_number")
    private String trackingNumber;
}
