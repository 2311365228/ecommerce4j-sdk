package com.ecommerce4j.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 平台返回的履约文档
 */
@Data
@Builder
public class FulfillmentDocument {

    /**
     * 平台包裹ID
     */
    private String packageId;

    /**
     * 文档二进制内容
     */
    private byte[] content;

    /**
     * 文档MIME类型
     */
    private String mimeType;
}
