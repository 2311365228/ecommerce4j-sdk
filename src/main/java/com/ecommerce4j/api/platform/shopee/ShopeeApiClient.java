package com.ecommerce4j.api.platform.shopee;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.ecommerce4j.api.platform.shopee.dto.ShopeeModels;
import com.ecommerce4j.api.platform.shopee.dto.ShopeeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Shopee OpenAPI v2 请求客户端
 * <p>
 * 负责统一处理网关域名、公共参数、签名、JSON 序列化以及平台错误响应
 */
class ShopeeApiClient {

    static final String PATH_AUTH_TOKEN_GET = "/api/v2/auth/token/get";
    static final String PATH_AUTH_ACCESS_TOKEN_GET = "/api/v2/auth/access_token/get";
    static final String PATH_SHOP_GET_INFO = "/api/v2/shop/get_shop_info";
    static final String PATH_ORDER_GET_LIST = "/api/v2/order/get_order_list";
    static final String PATH_ORDER_GET_DETAIL = "/api/v2/order/get_order_detail";
    static final String PATH_LOGISTICS_GET_SHIPPING_PARAMETER = "/api/v2/logistics/get_shipping_parameter";
    static final String PATH_LOGISTICS_SHIP_ORDER = "/api/v2/logistics/ship_order";
    static final String PATH_LOGISTICS_GET_TRACKING_NUMBER = "/api/v2/logistics/get_tracking_number";
    static final String PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_PARAMETER = "/api/v2/logistics/get_shipping_document_parameter";
    static final String PATH_LOGISTICS_CREATE_SHIPPING_DOCUMENT = "/api/v2/logistics/create_shipping_document";
    static final String PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_RESULT = "/api/v2/logistics/get_shipping_document_result";
    static final String PATH_LOGISTICS_DOWNLOAD_SHIPPING_DOCUMENT = "/api/v2/logistics/download_shipping_document";
    static final String PATH_LOGISTICS_GET_TRACKING_INFO = "/api/v2/logistics/get_tracking_info";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final OkHttpClient downloadClient;
    private final ObjectMapper objectMapper;
    private final long partnerId;
    private final String partnerKey;
    private final String gatewayBaseUrl;
    private final String authBaseUrl;

    ShopeeApiClient(OkHttpClient httpClient,
                    OkHttpClient downloadClient,
                    ObjectMapper objectMapper,
                    long partnerId,
                    String partnerKey,
                    String environment,
                    String gatewayRegion) {
        this(httpClient, downloadClient, objectMapper, partnerId, partnerKey, new EndpointConfig(
            resolveGatewayBaseUrl(environment, gatewayRegion),
            resolveAuthBaseUrl(environment, gatewayRegion)));
    }

    /**
     * 测试用入口：允许 MockWebServer 直接指定 API 网关和授权域名
     */
    static ShopeeApiClient forBaseUrls(OkHttpClient httpClient,
                                       OkHttpClient downloadClient,
                                       ObjectMapper objectMapper,
                                       long partnerId,
                                       String partnerKey,
                                       String gatewayBaseUrl,
                                       String authBaseUrl) {
        return new ShopeeApiClient(httpClient, downloadClient, objectMapper, partnerId, partnerKey, new EndpointConfig(gatewayBaseUrl, authBaseUrl));
    }

    private ShopeeApiClient(OkHttpClient httpClient,
                            OkHttpClient downloadClient,
                            ObjectMapper objectMapper,
                            long partnerId,
                            String partnerKey,
                            EndpointConfig endpointConfig) {
        this.httpClient = httpClient;
        this.downloadClient = downloadClient;
        this.objectMapper = objectMapper;
        this.partnerId = partnerId;
        this.partnerKey = partnerKey;
        this.gatewayBaseUrl = trimTrailingSlash(endpointConfig.gatewayBaseUrl);
        this.authBaseUrl = trimTrailingSlash(endpointConfig.authBaseUrl);
    }

    String getAuthorizationUrl(String redirectUri, String state) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(authBaseUrl), "【Shopee】授权 URL 无效")
            .newBuilder()
            .addQueryParameter("partner_id", String.valueOf(partnerId))
            .addQueryParameter("auth_type", "seller")
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("response_type", "code");
        if (StringUtils.hasText(state)) {
            builder.addQueryParameter("state", state);
        }
        return builder.build().toString();
    }

    ShopeeModels.TokenResponse getAccessToken(String code, String shopId, String mainAccountId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("partner_id", partnerId);
        putLongIfPresent(body, "shop_id", shopId);
        putLongIfPresent(body, "main_account_id", mainAccountId);
        return executePost(PATH_AUTH_TOKEN_GET, SignScope.PUBLIC, null, body, new TypeReference<>() {});
    }

    ShopeeModels.TokenResponse refreshAccessToken(AuthContext authContext) {
        if (authContext == null || !StringUtils.hasText(authContext.getRefreshToken())) {
            throw new EcommIntegrationException("【Shopee】刷新令牌（refresh_token）不能为空");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partner_id", partnerId);
        body.put("refresh_token", authContext.getRefreshToken());
        if (StringUtils.hasText(authContext.getShopId())) {
            putLongIfPresent(body, "shop_id", authContext.getShopId());
        } else if (StringUtils.hasText(authContext.getMerchantId())) {
            putLongIfPresent(body, "merchant_id", authContext.getMerchantId());
        } else {
            throw new EcommIntegrationException("【Shopee】刷新令牌时授权上下文必须包含店铺 ID（shopId）或商家 ID（merchantId）");
        }
        return executePost(PATH_AUTH_ACCESS_TOKEN_GET, SignScope.PUBLIC, null, body, new TypeReference<>() {});
    }

    ShopeeModels.ShopInfoResponse getShopInfo(AuthContext authContext) {
        return executeGet(PATH_SHOP_GET_INFO, SignScope.SHOP, authContext, Collections.emptyMap(), new TypeReference<>() {});
    }

    ShopeeModels.OrderListResponse getOrderList(AuthContext authContext, Map<String, String> queryParameters) {
        return executeGet(PATH_ORDER_GET_LIST, SignScope.SHOP, authContext, queryParameters, new TypeReference<>() {});
    }

    ShopeeModels.OrderDetailResponse getOrderDetail(AuthContext authContext,
                                                    List<String> orderSnList,
                                                    String responseOptionalFields,
                                                    boolean requestPendingStatus) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_sn_list", String.join(",", orderSnList));
        queryParameters.put("request_order_status_pending", String.valueOf(requestPendingStatus));
        if (StringUtils.hasText(responseOptionalFields)) {
            queryParameters.put("response_optional_fields", responseOptionalFields);
        }
        return executeGet(PATH_ORDER_GET_DETAIL, SignScope.SHOP, authContext, queryParameters, new TypeReference<>() {});
    }

    ShopeeModels.ShippingParameterResponse getShippingParameter(AuthContext authContext,
                                                                String orderSn,
                                                                String packageNumber) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_sn", orderSn);
        if (StringUtils.hasText(packageNumber)) {
            queryParameters.put("package_number", packageNumber);
        }
        return executeGet(PATH_LOGISTICS_GET_SHIPPING_PARAMETER, SignScope.SHOP, authContext, queryParameters, new TypeReference<>() {});
    }

    ShopeeResponse shipOrder(AuthContext authContext, ShopeeModels.ShipOrderRequest request) {
        return executePost(PATH_LOGISTICS_SHIP_ORDER, SignScope.SHOP, authContext, request, new TypeReference<>() {});
    }

    ShopeeModels.TrackingNumberResponse getTrackingNumber(AuthContext authContext,
                                                          String orderSn,
                                                          String packageNumber) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_sn", orderSn);
        if (StringUtils.hasText(packageNumber)) {
            queryParameters.put("package_number", packageNumber);
        }
        queryParameters.put("response_optional_fields", "first_mile_tracking_number");
        return executeGet(PATH_LOGISTICS_GET_TRACKING_NUMBER, SignScope.SHOP, authContext, queryParameters, new TypeReference<>() {});
    }

    ShopeeModels.ShippingDocumentParameterResponse getShippingDocumentParameter(AuthContext authContext,
                                                                                List<ShopeeModels.DocumentOrder> orders) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_list", orders);
        return executePost(PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_PARAMETER, SignScope.SHOP, authContext, body, new TypeReference<>() {});
    }

    ShopeeModels.ShippingDocumentOperationResponse createShippingDocument(AuthContext authContext,
                                                                          List<ShopeeModels.DocumentOrder> orders) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_list", orders);
        return executePost(PATH_LOGISTICS_CREATE_SHIPPING_DOCUMENT, SignScope.SHOP, authContext, body, new TypeReference<>() {});
    }

    ShopeeModels.ShippingDocumentOperationResponse getShippingDocumentResult(AuthContext authContext,
                                                                             List<ShopeeModels.DocumentOrder> orders) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_list", orders);
        return executePost(PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_RESULT, SignScope.SHOP, authContext, body, new TypeReference<>() {});
    }

    byte[] downloadShippingDocument(AuthContext authContext,
                                    String shippingDocumentType,
                                    List<ShopeeModels.DocumentOrder> orders) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.hasText(shippingDocumentType)) {
            body.put("shipping_document_type", shippingDocumentType);
        }
        body.put("order_list", orders);
        return executePostForBytes(PATH_LOGISTICS_DOWNLOAD_SHIPPING_DOCUMENT, authContext, body);
    }

    ShopeeModels.TrackingInfoResponse getTrackingInfo(AuthContext authContext, String orderSn, String packageNumber) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_sn", orderSn);
        if (StringUtils.hasText(packageNumber)) {
            queryParameters.put("package_number", packageNumber);
        }
        return executeGet(PATH_LOGISTICS_GET_TRACKING_INFO, SignScope.SHOP, authContext, queryParameters, new TypeReference<>() {});
    }

    String signPublic(String path, long timestamp) {
        return hmacSha256(String.valueOf(partnerId) + path + timestamp);
    }

    String signShop(String path, long timestamp, String accessToken, String shopId) {
        return hmacSha256(String.valueOf(partnerId) + path + timestamp + accessToken + shopId);
    }

    String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    String getAuthBaseUrl() {
        return authBaseUrl;
    }

    private <T extends ShopeeResponse> T executeGet(String path,
                                                    SignScope signScope,
                                                    AuthContext authContext,
                                                    Map<String, String> businessParameters,
                                                    TypeReference<T> typeReference) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> commonParameters = buildCommonParameters(path, signScope, authContext, timestamp);

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(gatewayBaseUrl + path), "【Shopee】请求 URL 无效").newBuilder();
        for (Map.Entry<String, String> entry : commonParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        if (businessParameters != null) {
            for (Map.Entry<String, String> entry : businessParameters.entrySet()) {
                if (StringUtils.hasText(entry.getValue())) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build();
        return executeJsonRequest(request, path, typeReference);
    }

    private <T extends ShopeeResponse> T executePost(String path,
                                                     SignScope signScope,
                                                     AuthContext authContext,
                                                     Object body,
                                                     TypeReference<T> typeReference) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> commonParameters = buildCommonParameters(path, signScope, authContext, timestamp);
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(gatewayBaseUrl + path), "【Shopee】请求 URL 无效").newBuilder();
        for (Map.Entry<String, String> entry : commonParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .post(RequestBody.create(objectToJson(body), JSON))
            .build();
        return executeJsonRequest(request, path, typeReference);
    }

    private byte[] executePostForBytes(String path, AuthContext authContext, Object body) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> commonParameters = buildCommonParameters(path, SignScope.SHOP, authContext, timestamp);
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(gatewayBaseUrl + path), "【Shopee】请求 URL 无效").newBuilder();
        for (Map.Entry<String, String> entry : commonParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .post(RequestBody.create(objectToJson(body), JSON))
            .build();

        try (Response response = downloadClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            byte[] bytes = responseBody == null ? new byte[0] : responseBody.bytes();
            if (!response.isSuccessful()) {
                throw new EcommIntegrationException("【Shopee】文件请求失败，接口=" + path + "，状态码=" + response.code() + "，响应=" + new String(bytes, StandardCharsets.UTF_8));
            }
            String contentType = response.header("Content-Type");
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
                ShopeeResponse parsed = objectMapper.readValue(bytes, ShopeeResponse.class);
                validateTopLevelResponse(parsed, path);
            }
            return bytes;
        } catch (IOException e) {
            throw new EcommIntegrationException("【Shopee】文件请求失败，接口=" + path, e);
        }
    }

    private <T extends ShopeeResponse> T executeJsonRequest(Request request,
                                                            String path,
                                                            TypeReference<T> typeReference) {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? null : responseBody.string();
            if (!response.isSuccessful()) {
                throw new EcommIntegrationException("【Shopee】接口请求失败，接口=" + path + "，状态码=" + response.code() + "，响应=" + responseText);
            }
            if (!StringUtils.hasText(responseText)) {
                throw new EcommIntegrationException("【Shopee】接口响应体为空，接口=" + path);
            }
            T parsed = objectMapper.readValue(responseText, typeReference);
            validateTopLevelResponse(parsed, path);
            return parsed;
        } catch (IOException e) {
            throw new EcommIntegrationException("【Shopee】接口请求失败，接口=" + path, e);
        }
    }

    private void validateTopLevelResponse(ShopeeResponse response, String path) {
        if (response == null) {
            throw new EcommIntegrationException("【Shopee】接口响应为空，接口=" + path);
        }
        if (StringUtils.hasText(response.getError())) {
            throw new EcommIntegrationException("【Shopee】接口调用失败，接口=" + path + "，错误码=" + response.getError() + "，错误信息=" + response.getMessage() + "，请求ID=" + response.getRequestId());
        }
    }

    private Map<String, String> buildCommonParameters(String path,
                                                      SignScope signScope,
                                                      AuthContext authContext,
                                                      long timestamp) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("partner_id", String.valueOf(partnerId));
        parameters.put("timestamp", String.valueOf(timestamp));
        if (signScope == SignScope.SHOP) {
            validateShopAuth(authContext);
            parameters.put("access_token", authContext.getAccessToken());
            parameters.put("shop_id", authContext.getShopId());
            parameters.put("sign", signShop(path, timestamp, authContext.getAccessToken(), authContext.getShopId()));
        } else {
            parameters.put("sign", signPublic(path, timestamp));
        }
        return parameters;
    }

    private void validateShopAuth(AuthContext authContext) {
        if (authContext == null || !StringUtils.hasText(authContext.getAccessToken()) || !StringUtils.hasText(authContext.getShopId())) {
            throw new EcommIntegrationException("【Shopee】店铺级 API 需要访问令牌（accessToken）和店铺 ID（shopId）");
        }
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyMap() : value);
        } catch (IOException e) {
            throw new EcommIntegrationException("【Shopee】序列化请求体失败", e);
        }
    }

    private String hmacSha256(String baseString) {
        if (!StringUtils.hasText(partnerKey)) {
            throw new EcommIntegrationException("【Shopee】合作伙伴密钥（partner_key）不能为空");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new EcommIntegrationException("【Shopee】请求签名失败", e);
        }
    }

    private static void putLongIfPresent(Map<String, Object> body, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            body.put(key, Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new EcommIntegrationException("【Shopee】" + key + " 必须是数字类型", e);
        }
    }

    private static String resolveGatewayBaseUrl(String environment, String gatewayRegion) {
        boolean sandbox = isSandbox(environment);
        String region = normalize(gatewayRegion);
        if (sandbox) {
            if ("cn".equals(region) || "china".equals(region) || "mainland_china".equals(region)) {
                return "https://openplatform.sandbox.test-stable.shopee.cn";
            }
            return "https://openplatform.sandbox.test-stable.shopee.sg";
        }
        if ("cn".equals(region) || "china".equals(region) || "mainland_china".equals(region)) {
            return "https://openplatform.shopee.cn";
        }
        if ("br".equals(region) || "brazil".equals(region)) {
            return "https://openplatform.shopee.com.br";
        }
        return "https://partner.shopeemobile.com";
    }

    private static String resolveAuthBaseUrl(String environment, String gatewayRegion) {
        boolean sandbox = isSandbox(environment);
        String region = normalize(gatewayRegion);
        String path = sandbox ? "/auth" : "/auth";
        if (sandbox) {
            if ("cn".equals(region) || "china".equals(region) || "mainland_china".equals(region)) {
                return "https://open.sandbox.test-stable.shopee.cn" + path;
            }
            if ("br".equals(region) || "brazil".equals(region)) {
                return "https://open.sandbox.test-stable.shopee.com.br" + path;
            }
            return "https://open.sandbox.test-stable.shopee.com" + path;
        }
        if ("cn".equals(region) || "china".equals(region) || "mainland_china".equals(region)) {
            return "https://open.shopee.cn" + path;
        }
        if ("br".equals(region) || "brazil".equals(region)) {
            return "https://open.shopee.com.br" + path;
        }
        return "https://open.shopee.com" + path;
    }

    private static boolean isSandbox(String environment) {
        String normalized = normalize(environment);
        return "sandbox".equals(normalized) || "test".equals(normalized) || "stable".equals(normalized) || "test_stable".equals(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private enum SignScope {
        PUBLIC,
        SHOP
    }

    private static class EndpointConfig {

        private final String gatewayBaseUrl;

        private final String authBaseUrl;

        private EndpointConfig(String gatewayBaseUrl, String authBaseUrl) {
            this.gatewayBaseUrl = gatewayBaseUrl;
            this.authBaseUrl = authBaseUrl;
        }
    }
}
