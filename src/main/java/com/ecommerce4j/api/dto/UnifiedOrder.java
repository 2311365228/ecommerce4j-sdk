package com.ecommerce4j.api.dto;

import com.ecommerce4j.api.enums.UnifiedOrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;


/**
 * 统一订单数据模型
 */
@Data
public class UnifiedOrder {
    /**
     * 平台原始订单ID
     */
    private String orderId;
    /**
     * 映射后的统一订单状态
     */
    private UnifiedOrderStatus unifiedStatus;
    /**
     * 平台原始订单状态
     */
    private String originalStatus;
    /**
     * 订单创建时间
     */
    private Instant createTime;
    /**
     * 订单最后更新时间
     */
    private Instant updateTime;
    /**
     * 交易货币单位 (ISO 4217)
     */
    private String currency;
    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;
    /**
     * 买家信息 (通常是昵称或加密ID)
     */
    private String buyerInfo;
    /**
     * 订单包含的商品行项目列表
     */
    private List<UnifiedOrderItem> orderItems;
    /**
     * 订单关联的货运信息
     */
    private UnifiedShipment shipment;
    /**
     * 可选字段: 用于存储平台返回的原始JSON数据，便于调试和追溯
     */
    private Map<String, Object> rawData;
}
