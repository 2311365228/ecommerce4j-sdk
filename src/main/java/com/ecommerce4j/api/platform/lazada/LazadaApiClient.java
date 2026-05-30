package com.ecommerce4j.api.platform.lazada;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.ecommerce4j.api.platform.lazada.dto.LazadaAuthModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaFulfillmentModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaOrderModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaResponse;
import com.ecommerce4j.api.platform.lazada.dto.LazadaTraceModels;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Lazada Open Platform 请求客户端
 */
class LazadaApiClient {

    private static final String SIGN_METHOD = "sha256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final Map<String, String> DEFAULT_SITE_ENDPOINTS = Map.of(
        "sg", "https://api.lazada.sg/rest",
        "my", "https://api.lazada.com.my/rest",
        "ph", "https://api.lazada.com.ph/rest",
        "th", "https://api.lazada.co.th/rest",
        "id", "https://api.lazada.co.id/rest",
        "vn", "https://api.lazada.vn/rest"
    );

    private final OkHttpClient httpClient;
    private final OkHttpClient downloadClient;
    private final ObjectMapper objectMapper;
    private final String appKey;
    private final String appSecret;
    private final String authBaseUrl;
    private final Map<String, String> siteEndpoints;

    LazadaApiClient(OkHttpClient httpClient,
                    OkHttpClient downloadClient,
                    ObjectMapper objectMapper,
                    String appKey,
                    String appSecret,
                    String authBaseUrl) {
        this(httpClient, downloadClient, objectMapper, appKey, appSecret, authBaseUrl, Collections.emptyMap());
    }

    LazadaApiClient(OkHttpClient httpClient,
                    OkHttpClient downloadClient,
                    ObjectMapper objectMapper,
                    String appKey,
                    String appSecret,
                    String authBaseUrl,
                    Map<String, String> siteEndpoints) {
        this.httpClient = httpClient;
        this.downloadClient = downloadClient;
        this.objectMapper = objectMapper;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.authBaseUrl = authBaseUrl;
        this.siteEndpoints = new HashMap<>(DEFAULT_SITE_ENDPOINTS);
        if (siteEndpoints != null) {
            for (Map.Entry<String, String> entry : siteEndpoints.entrySet()) {
                if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                    this.siteEndpoints.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
                }
            }
        }
    }

    LazadaAuthModels.TokenResponse exchangeCodeForTokens(String code) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("code", code);
        return executePost(authBaseUrl, "/auth/token/create", buildCommonParameters(null), parameters, new TypeReference<>() {});
    }

    LazadaAuthModels.TokenResponse refreshTokens(String refreshToken) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("refresh_token", refreshToken);
        return executePost(authBaseUrl, "/auth/token/refresh", buildCommonParameters(null), parameters, new TypeReference<>() {});
    }

    LazadaAuthModels.SellerResponse getSeller(AuthContext authContext) {
        return executeGet(resolveSiteBaseUrl(authContext), "/seller/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), Collections.emptyMap(), new TypeReference<>() {});
    }

    LazadaOrderModels.OrdersResponse getOrders(AuthContext authContext, Map<String, String> queryParameters) {
        return executeGet(resolveSiteBaseUrl(authContext), "/orders/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), queryParameters, new TypeReference<>() {});
    }

    LazadaOrderModels.OrderResponse getOrder(AuthContext authContext, String orderId) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_id", orderId);
        return executeGet(resolveSiteBaseUrl(authContext), "/order/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), queryParameters, new TypeReference<>() {});
    }

    LazadaOrderModels.OrderItemsResponse getOrderItems(AuthContext authContext, String orderId) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_id", orderId);
        return executeGet(resolveSiteBaseUrl(authContext), "/order/items/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), queryParameters, new TypeReference<>() {});
    }

    LazadaOrderModels.MultipleOrderItemsResponse getMultipleOrderItems(AuthContext authContext, List<String> orderIds) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_ids", objectToJson(orderIds));
        return executeGet(resolveSiteBaseUrl(authContext), "/orders/items/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), queryParameters, new TypeReference<>() {});
    }

    LazadaFulfillmentModels.ShipmentProvidersResponse getShipmentProviders(AuthContext authContext,
                                                                           LazadaFulfillmentModels.ShipmentProvidersRequest request) {
        return executePost(resolveSiteBaseUrl(authContext), "/order/shipment/providers/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), Collections.singletonMap("getShipmentProvidersReq", objectToJson(request)), new TypeReference<>() {});
    }

    LazadaFulfillmentModels.PackResponse pack(AuthContext authContext, LazadaFulfillmentModels.PackRequest request) {
        return executePost(resolveSiteBaseUrl(authContext), "/order/fulfill/pack", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), Collections.singletonMap("packReq", objectToJson(request)), new TypeReference<>() {});
    }

    LazadaFulfillmentModels.PackageDocumentResponse getPackageDocument(AuthContext authContext,
                                                                       LazadaFulfillmentModels.PackageDocumentRequest request) {
        return executePost(resolveSiteBaseUrl(authContext), "/order/package/document/get", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), Collections.singletonMap("getDocumentReq", objectToJson(request)), new TypeReference<>() {});
    }

    LazadaFulfillmentModels.ReadyToShipResponse readyToShip(AuthContext authContext, LazadaFulfillmentModels.ReadyToShipRequest request) {
        return executePost(resolveSiteBaseUrl(authContext), "/order/package/rts", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), Collections.singletonMap("readyToShipReq", objectToJson(request)), new TypeReference<>() {});
    }

    LazadaTraceModels.OrderTraceResponse getOrderTrace(AuthContext authContext,
                                                       String orderId,
                                                       List<String> packageIds,
                                                       String locale) {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("order_id", orderId);
        if (packageIds != null && !packageIds.isEmpty()) {
            queryParameters.put("ofcPackageIdList", objectToJson(packageIds));
        }
        queryParameters.put("locale", StringUtils.hasText(locale) ? locale : "en");
        return executeGet(resolveSiteBaseUrl(authContext), "/logistic/order/trace", buildCommonParameters(authContext == null ? null : authContext.getAccessToken()), queryParameters, new TypeReference<>() {});
    }

    byte[] downloadPdf(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                String bodyText = responseBody == null ? "" : responseBody.string();
                throw new EcommIntegrationException("【Lazada】下载 PDF 失败，url=" + url + "，status=" + response.code() + "，body=" + bodyText);
            }
            return Objects.requireNonNull(response.body(), "【Lazada】下载 PDF 响应体为空。").bytes();
        } catch (IOException e) {
            throw new EcommIntegrationException("【Lazada】下载 PDF 失败，url=" + url, e);
        }
    }

    byte[] decodeDocumentFile(String fileContent) {
        try {
            return Base64.getDecoder().decode(fileContent);
        } catch (IllegalArgumentException e) {
            throw new EcommIntegrationException("【Lazada】无法解析面单文件内容，请优先使用 pdf_url 下载。", e);
        }
    }

    private <T extends LazadaResponse> T executeGet(String baseUrl,
                                                    String path,
                                                    Map<String, String> commonParameters,
                                                    Map<String, String> businessParameters,
                                                    TypeReference<T> typeReference) {
        Map<String, String> requestParameters = new LinkedHashMap<>(commonParameters);
        requestParameters.putAll(businessParameters);
        requestParameters.put("sign", sign(path, requestParameters));

        Request request;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(baseUrl + path), "【Lazada】URL 无效。").newBuilder();
        for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        request = new Request.Builder().url(urlBuilder.build()).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? null : responseBody.string();
            if (!response.isSuccessful()) {
                throw new EcommIntegrationException("【Lazada】请求失败，path=" + path + "，status=" + response.code() + "，body=" + responseText);
            }
            if (!StringUtils.hasText(responseText)) {
                throw new EcommIntegrationException("【Lazada】接口响应体为空，path=" + path);
            }
            T parsed = objectMapper.readValue(responseText, typeReference);
            validateTopLevelResponse(parsed, path);
            return parsed;
        } catch (IOException e) {
            throw new EcommIntegrationException("【Lazada】请求失败，path=" + path, e);
        }
    }

    private <T extends LazadaResponse> T executePost(String baseUrl,
                                                     String path,
                                                     Map<String, String> commonParameters,
                                                     Map<String, String> businessParameters,
                                                     TypeReference<T> typeReference) {
        Map<String, String> signParameters = new LinkedHashMap<>(commonParameters);
        signParameters.putAll(businessParameters);
        String sign = sign(path, signParameters);

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(baseUrl + path), "【Lazada】URL 无效。").newBuilder();
        for (Map.Entry<String, String> entry : commonParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        urlBuilder.addQueryParameter("sign", sign);

        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : businessParameters.entrySet()) {
            bodyBuilder.add(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .post(bodyBuilder.build())
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseText = responseBody == null ? null : responseBody.string();
            if (!response.isSuccessful()) {
                throw new EcommIntegrationException("【Lazada】请求失败，path=" + path + "，status=" + response.code() + "，body=" + responseText);
            }
            if (!StringUtils.hasText(responseText)) {
                throw new EcommIntegrationException("【Lazada】接口响应体为空，path=" + path);
            }
            T parsed = objectMapper.readValue(responseText, typeReference);
            validateTopLevelResponse(parsed, path);
            return parsed;
        } catch (IOException e) {
            throw new EcommIntegrationException("【Lazada】请求失败，path=" + path, e);
        }
    }

    private void validateTopLevelResponse(LazadaResponse response, String path) {
        if (response == null) {
            throw new EcommIntegrationException("【Lazada】接口响应为空，path=" + path);
        }
        if (!"0".equals(response.getCode())) {
            StringBuilder builder = new StringBuilder("【Lazada】接口调用失败，path=")
                .append(path)
                .append("，code=")
                .append(response.getCode());
            if (StringUtils.hasText(response.getMessage())) {
                builder.append("，message=").append(response.getMessage());
            }
            if (StringUtils.hasText(response.getType())) {
                builder.append("，type=").append(response.getType());
            }
            if (StringUtils.hasText(response.getRequestId())) {
                builder.append("，requestId=").append(response.getRequestId());
            }
            throw new EcommIntegrationException(builder.toString());
        }
    }

    private Map<String, String> buildCommonParameters(String accessToken) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("app_key", appKey);
        parameters.put("timestamp", String.valueOf(System.currentTimeMillis()));
        parameters.put("sign_method", SIGN_METHOD);
        if (StringUtils.hasText(accessToken)) {
            parameters.put("access_token", accessToken);
        }
        return parameters;
    }

    private String resolveSiteBaseUrl(AuthContext authContext) {
        if (authContext == null || !StringUtils.hasText(authContext.getSiteCountry())) {
            throw new EcommIntegrationException("【Lazada】AuthContext 缺少 siteCountry，无法确定站点网关。");
        }
        String siteCountry = authContext.getSiteCountry().trim().toLowerCase(Locale.ROOT);
        String baseUrl = siteEndpoints.get(siteCountry);
        if (!StringUtils.hasText(baseUrl)) {
            throw new EcommIntegrationException("【Lazada】不支持的站点 country=" + authContext.getSiteCountry());
        }
        return baseUrl;
    }

    private String sign(String path, Map<String, String> parameters) {
        TreeMap<String, String> sortedParameters = new TreeMap<>(parameters);
        StringBuilder builder = new StringBuilder(path);
        for (Map.Entry<String, String> entry : sortedParameters.entrySet()) {
            if ("sign".equals(entry.getKey())) {
                continue;
            }
            builder.append(entry.getKey()).append(entry.getValue());
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02X", value));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new EcommIntegrationException("【Lazada】签名失败，path=" + path, e);
        }
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new EcommIntegrationException("【Lazada】序列化请求参数失败。", e);
        }
    }
}
