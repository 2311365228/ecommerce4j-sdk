package com.ecommerce4j.api.dto;

import lombok.Data;

/**
 * 统一地址数据模型
 */
@Data
public class UnifiedAddress {
    /**
     * 收件人全名
     */
    private String fullName;
    /**
     * 收件人联系电话
     */
    private String phone;
    /**
     * 国家/地区代码 (ISO 3166-1 alpha-2)
     */
    private String countryCode;
    /**
     * 省/州
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 区/县
     */
    private String district;
    /**
     * 街道地址详情
     */
    private String street;
    /**
     * 邮政编码
     */
    private String zipCode;
}
