package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Mercado Libre 订单搜索响应数据类。
 * 封装了从 Mercado Libre API 搜索订单列表时返回的完整数据结构。
 */
@Data
public class MercadoLibreOrderSearchResponse {
    /**
     * 查询关键词或参数。
     */
    private String query;
    /**
     * 分页信息。
     */
    private Paging paging;
    /**
     * 订单结果列表。
     */
    private List<MercadoLibreOrder> results;

    /**
     * 分页信息内部类。
     * 描述了分页查询的总数、偏移量和限制。
     */
    @Data
    public static class Paging {
        /**
         * 总记录数。
         */
        private int total;
        /**
         * 当前查询的偏移量。
         */
        private int offset;
        /**
         * 每页记录数限制。
         */
        private int limit;
    }
}
