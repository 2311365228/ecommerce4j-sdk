package com.ecommerce4j.api.enums;

/**
 * 履约类型
 */
public enum FulfillmentType {

    /**
     * 指示WMS需要从平台下载面单文件进行打印。
     * (适用于平台物流, 如 TikTok Shipping, Mercado Libre ME2)
     */
    DOWNLOAD_LABEL,

    /**
     * 指示WMS需要使用自有的物流渠道发货，然后将运单信息回传给平台。
     * (适用于卖家自发货, 如 Seller Shipping)
     */
    PROVIDE_TRACKING,

    /**
     * 履约前发生错误，无法进行下一步操作。
     */
    ERROR

}
