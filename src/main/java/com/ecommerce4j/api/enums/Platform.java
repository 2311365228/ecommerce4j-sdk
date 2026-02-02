package com.ecommerce4j.api.enums;

import lombok.Getter;

/**
 * 电商平台枚举
 */
@Getter
public enum Platform {
    TIKTOK_SHOP("TikTok Shop"),

    MERCADO_LIBRE("Mercado Libre"),

    /**
     * 希音墨西哥自运营
     */
    SHEIN_MX_SELF("Shein-MX-Self")
    ;

    private final String description;

    Platform(String description) {
        this.description = description;
    }

}
