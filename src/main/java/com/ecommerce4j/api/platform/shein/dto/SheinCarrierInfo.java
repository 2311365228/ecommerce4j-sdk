package com.ecommerce4j.api.platform.shein.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * SHEIN 物流商数据
 */
@Data
public class SheinCarrierInfo {

    @JsonAlias({"carrierCode", "carrierId", "logisticsProviderCode", "providerCode", "companyCode"})
    private String carrierId;

    @JsonAlias({"carrierName", "logisticsProviderName", "providerName", "companyName"})
    private String carrierName;
}
