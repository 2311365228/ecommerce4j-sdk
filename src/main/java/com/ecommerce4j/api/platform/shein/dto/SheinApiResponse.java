package com.ecommerce4j.api.platform.shein.dto;

import lombok.Data;

/**
 * SHEIN 开放平台通用响应模型。
 */
@Data
public class SheinApiResponse<T> {

    /**
     * 平台返回码。
     */
    private Object code;

    /**
     * 常见的错误消息字段。
     */
    private String msg;

    /**
     * 部分接口使用 message 作为消息字段。
     */
    private String message;

    /**
     * 部分接口使用 errorMsg 作为错误消息字段。
     */
    private String errorMsg;

    /**
     * 部分接口直接返回布尔型成功标记。
     */
    private Boolean success;

    private T data;

    private T info;

    private T result;
}
