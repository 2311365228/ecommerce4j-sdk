package com.ecommerce4j.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuery {
    /**
     * 订单创建时间的起始点 (包含)
     */
    private Instant createTimeFrom;
    /**
     * 订单创建时间的结束点 (不包含)
     */
    private Instant createTimeTo;
    /**
     * 按平台特定的订单状态进行过滤
     */
    private String orderStatus;
    /**
     * 每页返回的记录数
     */
    private int pageSize;
    /**
     * 用于获取下一页数据的分页符 (例如 TikTok 的 page_token 或 Mercado Libre 的 offset)
     */
    private String pageToken;

    /**
     * 是否过滤full仓（目前对于Mercado）的订单
     */
    private boolean filterFullStock;
}
