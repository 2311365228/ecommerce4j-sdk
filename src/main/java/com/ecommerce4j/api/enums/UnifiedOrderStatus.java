package com.ecommerce4j.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 */
@Getter
@AllArgsConstructor
public enum UnifiedOrderStatus {

    PENDING_APPROVAL("等待审批"),

    PENDING_PAYMENT("等待付款"),

    READY_FOR_FULFILLMENT("准备发货/履约"),

    IN_TRANSIT("运输中"),

    DELIVERED("已妥投"),

    CANCELLED("已取消"),

    COMPLETED("已完成"),

    UNKNOWN("未知")
    ;

    /**
     * 描述
     */
    private final String description;

}
