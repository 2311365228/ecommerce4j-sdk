package com.ecommerce4j.api;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.FulfillmentAction;
import com.ecommerce4j.api.dto.TrackingInfo;

/**
 * 统一履约服务接口
 * <p>
 * 负责处理订单的履约操作，如下载面单、提交物流信息等。
 */
public interface EcommFulfillmentService {

    /**
     * 准备订单履约，返回下一步需要执行的动作。
     * <p>
     * 这是实现统一履约流程的核心方法。它会根据平台的物流模式（如平台物流 vs. 卖家自发货）
     * 返回一个指令性对象 {@link FulfillmentAction}，指导WMS进行下一步操作。
     *
     * @param authContext 授权上下文
     * @param orderId     平台的原始订单ID
     * @param autoShipIfMissing true: 若待发货且无面单，自动尝试调用发货接口; false: 仅获取面单，不执行发货动作（并非所有电商平台有效）
     * @return 履约动作对象，指导WMS是应该下载面单还是提供物流信息
     */
    FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing);

    /**
     * 提交物流跟踪信息（适用于卖家自发货模式）。
     *
     * @param authContext  授权上下文
     * @param orderId      平台的原始订单ID
     * @param trackingInfo 包含运单号和物流商信息的对象
     */
    void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo);
}
