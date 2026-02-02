package com.ecommerce4j.api.platform.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MercadoLibreItem {

    /**
     * 商品的唯一标识符。
     */
    @JsonProperty("id")
    private String id;

    /**
     * 商品标题。
     */
    @JsonProperty("title")
    private String title;

    /**
     * 商品价格。
     */
    @JsonProperty("price")
    private Double price;

    /**
     * 货币单位ID (例如: "BRL", "ARS")。
     */
    @JsonProperty("currency_id")
    private String currencyId;

    /**
     * 可售数量。
     */
    @JsonProperty("available_quantity")
    private Integer availableQuantity;

    /**
     * 商品主图的URL。
     */
    @JsonProperty("thumbnail")
    private String thumbnail;

    /**
     * 商品在Mercado Libre上的永久链接。
     */
    @JsonProperty("permalink")
    private String permalink;

    /**
     * 商品图片列表。
     */
    @JsonProperty("pictures")
    private List<Picture> pictures;

    /**
     * 商品的变体信息列表（例如不同颜色、尺寸）。
     */
    @JsonProperty("variations")
    private List<Variation> variations;

    /**
     * 商品图片内部类。
     */
    @Data
    public static class Picture {
        /**
         * 图片的唯一ID。
         */
        @JsonProperty("id")
        private String id;

        /**
         * 安全的图片URL (HTTPS)。
         */
        @JsonProperty("secure_url")
        private String secureUrl;
    }

    /**
     * 商品变体内部类。
     */
    @Data
    public static class Variation {
        /**
         * 变体的唯一ID。
         */
        @JsonProperty("id")
        private Long id;

        /**
         * 变体价格。
         */
        @JsonProperty("price")
        private Double price;

        /**
         * 变体属性组合。
         */
        @JsonProperty("attribute_combinations")
        private List<AttributeCombination> attributeCombinations;

        /**
         * 变体可售数量。
         */
        @JsonProperty("available_quantity")
        private Integer availableQuantity;

        /**
         * 变体对应的图片ID列表。
         */
        @JsonProperty("picture_ids")
        private List<String> pictureIds;
    }

    /**
     * 变体属性组合内部类。
     */
    @Data
    public static class AttributeCombination {
        /**
         * 属性名称 (例如: "Color")。
         */
        @JsonProperty("name")
        private String name;

        /**
         * 属性值 (例如: "Black")。
         */
        @JsonProperty("value_name")
        private String valueName;
    }

}
