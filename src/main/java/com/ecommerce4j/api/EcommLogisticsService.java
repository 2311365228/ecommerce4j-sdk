package com.ecommerce4j.api;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.UnifiedShipment;

/**
 * 统一物流服务接口
 * <p>
 * 负责发货后的物流跟踪。
 */
public interface EcommLogisticsService {

    /**
     * 获取指定订单的物流轨迹事件。
     *
     * @param authContext 授权上下文
     * @param orderId     平台的原始订单ID
     * @return 包含完整物流事件的统一货运对象
     */
    UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId);
}
