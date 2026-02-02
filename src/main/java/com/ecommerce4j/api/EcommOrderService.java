package com.ecommerce4j.api;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.OrderQuery;
import com.ecommerce4j.api.dto.PaginatedResult;
import com.ecommerce4j.api.dto.UnifiedOrder;

import java.util.List;

/**
 * 统一订单服务接口
 * <p>
 * 负责所有订单相关的操作
 */
public interface EcommOrderService {

    /**
     * 根据查询条件获取订单列表。
     *
     * @param authContext 授权上下文，包含访问API所需的令牌
     * @param query       订单查询参数对象
     * @return 包含订单列表和分页令牌的统一分页结果
     */
    PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query);

    /**
     * 获取一个或多个订单的详细信息。
     *
     * @param authContext 授权上下文
     * @param orderIds    平台的原始订单ID列表
     * @return 包含订单详细信息的统一订单对象列表
     */
    List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds);
}
