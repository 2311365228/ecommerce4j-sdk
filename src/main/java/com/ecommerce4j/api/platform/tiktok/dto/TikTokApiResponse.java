package com.ecommerce4j.api.platform.tiktok.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TikTok API 响应的通用外层结构。
 *
 * @param <T> data 字段的具体类型
 */
@Data
public class TikTokApiResponse<T> {
    /**
     * 响应码，0 表示成功
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 请求ID，用于问题排查
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 实际的业务数据
     */
    private T data;
}
