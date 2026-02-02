package com.ecommerce4j.api.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UnifiedOrderItem {
    /**
     * 平台原始订单行项目ID
     */
    private String orderLineId;

    /**
     * 商品id
     */
    private String productId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 平台skuId，用于区分是否属于同一个产品
     */
    private String skuId;
    /**
     * 商品SKU名称
     */
    private String skuName;
    /**
     * 商品图片URL
     */
    private String imageUrl;
    /**
     * 购买数量
     */
    private Integer quantity;
    /**
     * 商品单价
     */
    private BigDecimal unitPrice;
}
