package com.ecommerce4j.api.dto;

import com.ecommerce4j.api.enums.FulfillmentType;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FulfillmentAction {
    /**
     * WMS需要执行的履约动作类型
     */
    private FulfillmentType fulfillmentType;
    /**
     * 当类型为 DOWNLOAD_LABEL 时，此字段包含面单文件的二进制内容
     */
    private byte[] labelContent;
    /**
     * 物流追踪号码，面单上的物流单号
     */
    private String trackingNumber;
    /**
     * 收货人名称
     */
    private String receiverName;
    /**
     * 面单文件的MIME类型, 例如 "application/pdf" 或 "application/zpl"
     */
    private String labelMimeType;
    /**
     * 当类型为 PROVIDE_TRACKING 时，此字段包含平台可接受的物流商列表
     */
    private List<Map<String, String>> availableCarriers;
    /**
     * 如果履约准备失败，此字段包含错误信息
     */
    private String errorMessage;
}
