package com.ecommerce4j.api.platform.shein;

import com.ecommerce4j.api.EcommAuthorizationService;
import com.ecommerce4j.api.EcommFulfillmentService;
import com.ecommerce4j.api.EcommLogisticsService;
import com.ecommerce4j.api.EcommOrderService;
import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.FulfillmentAction;
import com.ecommerce4j.api.dto.OrderQuery;
import com.ecommerce4j.api.dto.PaginatedResult;
import com.ecommerce4j.api.dto.TrackingInfo;
import com.ecommerce4j.api.dto.UnifiedAddress;
import com.ecommerce4j.api.dto.UnifiedOrder;
import com.ecommerce4j.api.dto.UnifiedOrderItem;
import com.ecommerce4j.api.dto.UnifiedShipment;
import com.ecommerce4j.api.dto.UnifiedShopInfo;
import com.ecommerce4j.api.dto.UnifiedTrackingEvent;
import com.ecommerce4j.api.enums.FulfillmentType;
import com.ecommerce4j.api.enums.Platform;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.ecommerce4j.api.platform.AbstractAdapter;
import com.ecommerce4j.api.platform.shein.dto.SheinApiResponse;
import com.ecommerce4j.api.platform.shein.dto.SheinCarrierInfo;
import com.ecommerce4j.api.platform.shein.dto.SheinCredentialData;
import com.ecommerce4j.api.platform.shein.dto.SheinStatusMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * SHEIN 墨西哥自运营店铺适配器
 */
@Slf4j
@Service("SHEIN_MX_SELF")
public class SheinSelfOperatedAdapter extends AbstractAdapter implements EcommOrderService, EcommFulfillmentService, EcommLogisticsService, EcommAuthorizationService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_AUTH_PATH = "/open-api/auth/get-by-token";
    private static final String DEFAULT_ORDER_SEARCH_PATH = "/open-api/order/search";
    private static final String DEFAULT_ORDER_DETAIL_PATH = "/open-api/order/details";
    private static final String DEFAULT_TRACKING_SUBMIT_PATH = "/open-api/order/shipping";
    private static final String DEFAULT_TRACKING_QUERY_PATH = "/open-api/order/tracking";
    private static final String DEFAULT_CARRIER_LIST_PATH = "/open-api/logistics/carriers";
    private static final String DEFAULT_SHOP_INFO_PATH = "/open-api/shop/info";
    private static final String RANDOM_KEY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstvuwxyz1234567890";
    private static final Random RANDOM = new Random();

    @Value("${shein.app_id}")
    private String appId;

    @Value("${shein.app_secret}")
    private String appSecret;

    @Value("${shein.auth_url:https://open.sheincorp.com}")
    private String authUrl;

    @Value("${shein.redirect_uri:}")
    private String redirectUri;

    @Value("${shein.api_base_url:https://openapi.sheincorp.com}")
    private String apiBaseUrl;

    @Value("${shein.auth_path:" + DEFAULT_AUTH_PATH + "}")
    private String authPath;

    @Value("${shein.order_search_path:" + DEFAULT_ORDER_SEARCH_PATH + "}")
    private String orderSearchPath;

    @Value("${shein.order_detail_path:" + DEFAULT_ORDER_DETAIL_PATH + "}")
    private String orderDetailPath;

    @Value("${shein.tracking_submit_path:" + DEFAULT_TRACKING_SUBMIT_PATH + "}")
    private String trackingSubmitPath;

    @Value("${shein.tracking_query_path:" + DEFAULT_TRACKING_QUERY_PATH + "}")
    private String trackingQueryPath;

    @Value("${shein.carrier_list_path:" + DEFAULT_CARRIER_LIST_PATH + "}")
    private String carrierListPath;

    @Value("${shein.shop_info_path:" + DEFAULT_SHOP_INFO_PATH + "}")
    private String shopInfoPath;

    /**
     * 授权页路径
     */
    @Value("${shein.authorize_path:/#/empower}")
    private String authorizePath;

    /**
     * 获取订单列表。
     */
    @Override
    public PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query) {
        validateMerchantCredentials(authContext);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageNo", parsePageNo(query.getPageToken()));
        body.put("pageSize", query.getPageSize() > 0 ? query.getPageSize() : 50);
        if (query.getCreateTimeFrom() != null) {
            body.put("startTime", query.getCreateTimeFrom().toEpochMilli());
        }
        if (query.getCreateTimeTo() != null) {
            body.put("endTime", query.getCreateTimeTo().toEpochMilli());
        }
        if (StringUtils.hasText(query.getOrderStatus())) {
            body.put("orderStatus", query.getOrderStatus());
        }

        Map<String, Object> payload = executeSignedPost(authContext, normalizeApiPath(orderSearchPath), body);
        List<UnifiedOrder> orders = extractOrderList(payload).stream()
            .map(this::mapToUnifiedOrder)
            .collect(Collectors.toList());

        String nextPageToken = resolveNextPageToken(payload, query.getPageToken(), orders.size(), query.getPageSize());
        return new PaginatedResult<>(orders, nextPageToken);
    }

    /**
     * 获取订单详情。
     */
    @Override
    public List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds) {
        validateMerchantCredentials(authContext);
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderIds", orderIds);

        Map<String, Object> payload = executeSignedPost(authContext, normalizeApiPath(orderDetailPath), body);
        return extractOrderList(payload).stream()
            .map(this::mapToUnifiedOrder)
            .collect(Collectors.toList());
    }

    /**
     * SHEIN 自运营场景更常见的是商家回填物流单号，因此默认返回 PROVIDE_TRACKING。
     */
    @Override
    public FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing) {
        validateMerchantCredentials(authContext);
        List<UnifiedOrder> orders = getOrderDetails(authContext, Collections.singletonList(orderId));
        if (CollectionUtils.isEmpty(orders)) {
            throw new EcommIntegrationException("【SHEIN】未找到订单: " + orderId);
        }

        UnifiedOrder order = orders.get(0);
        List<Map<String, String>> carriers = loadCarrierOptions(authContext);

        return FulfillmentAction.builder()
            .fulfillmentType(FulfillmentType.PROVIDE_TRACKING)
            .receiverName(Optional.ofNullable(order.getShipment())
                .map(UnifiedShipment::getShippingAddress)
                .map(UnifiedAddress::getFullName)
                .orElse(""))
            .availableCarriers(carriers)
            .errorMessage(carriers.isEmpty() ? "【SHEIN】未从平台查询到可用物流商，请根据控制台文档确认物流商列表接口路径。" : null)
            .build();
    }

    /**
     * 提交物流单号。
     */
    @Override
    public void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo) {
        validateMerchantCredentials(authContext);
        if (trackingInfo == null || !StringUtils.hasText(trackingInfo.getTrackingNumber())) {
            throw new EcommIntegrationException("【SHEIN】trackingNumber 不能为空。");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", orderId);
        body.put("trackingNumber", trackingInfo.getTrackingNumber());
        if (StringUtils.hasText(trackingInfo.getShippingProviderId())) {
            body.put("carrierCode", trackingInfo.getShippingProviderId());
        }
        if (!CollectionUtils.isEmpty(trackingInfo.getOrderLineItemIds())) {
            body.put("orderLineIds", trackingInfo.getOrderLineItemIds());
        }

        executeSignedPost(authContext, normalizeApiPath(trackingSubmitPath), body);
    }

    /**
     * 查询物流轨迹。
     */
    @Override
    public UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId) {
        validateMerchantCredentials(authContext);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", orderId);

        Map<String, Object> payload = executeSignedPost(authContext, normalizeApiPath(trackingQueryPath), body);
        UnifiedShipment shipment = new UnifiedShipment();

        Map<String, Object> shipmentMap = firstMap(payload, "shipment", "delivery", "logistics", "data");
        if (shipmentMap == null) {
            return shipment;
        }

        shipment.setShipmentId(readString(shipmentMap, "shipmentId", "deliveryId", "packageId"));
        shipment.setTrackingNumber(readString(shipmentMap, "trackingNumber", "trackingNo", "waybillNo"));
        shipment.setCarrier(readString(shipmentMap, "carrierName", "logisticsProviderName", "companyName"));
        shipment.setOriginalStatus(readString(shipmentMap, "shipmentStatus", "status"));
        shipment.setUnifiedStatus(readString(shipmentMap, "shipmentStatus", "status"));
        shipment.setShippingAddress(mapToAddress(firstMap(shipmentMap, "address", "receiverAddress")));

        List<UnifiedTrackingEvent> events = extractMapList(shipmentMap, "trackingList", "tracks", "traceList", "events")
            .stream()
            .map(this::mapToTrackingEvent)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(UnifiedTrackingEvent::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
        shipment.setTrackingEvents(events);
        return shipment;
    }

    /**
     * 生成商家授权入口。
     */
    @Override
    public String getAuthorizationUrl(String state) {
        HttpUrl base = HttpUrl.parse(authUrl);
        if (base == null) {
            throw new EcommIntegrationException("【SHEIN】auth_url 配置无效: " + authUrl);
        }

        HttpUrl.Builder builder;
        if (StringUtils.hasText(authorizePath) && authorizePath.startsWith("/#")) {
            String normalized = authUrl.endsWith("/") ? authUrl.substring(0, authUrl.length() - 1) : authUrl;
            String raw = normalized + authorizePath;
            builder = Objects.requireNonNull(HttpUrl.parse(raw), "SHEIN authorize url invalid").newBuilder();
        } else if (StringUtils.hasText(authorizePath)) {
            builder = base.newBuilder().addPathSegments(trimLeadingSlash(authorizePath));
        } else {
            builder = base.newBuilder();
        }

        builder.addQueryParameter("appid", appId);
        if (StringUtils.hasText(redirectUri)) {
            builder.addQueryParameter("redirectUrl", redirectUri);
        }
        if (StringUtils.hasText(state)) {
            builder.addQueryParameter("state", state);
        }
        return builder.build().toString();
    }

    /**
     * 授权码换取凭证。
     */
    @Override
    public AuthContext exchangeCodeForTokens(String code) {
        if (!StringUtils.hasText(code)) {
            throw new EcommIntegrationException("【SHEIN】授权码不能为空。");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", code);
        if (StringUtils.hasText(redirectUri)) {
            body.put("redirectUrl", redirectUri);
        }

        SheinCredentialData credentialData = executeSignedPostForData(null, normalizeApiPath(authPath), body, appId, appSecret, new TypeReference<SheinApiResponse<SheinCredentialData>>() {});
        return mapToAuthContext(credentialData);
    }

    @Override
    public AuthContext refreshTokens(AuthContext authContext) {
        if (!StringUtils.hasText(authContext.getRefreshToken())) {
            throw new EcommIntegrationException("【SHEIN】当前授权上下文不包含 refreshToken，请按商家控制台文档确认刷新接口。");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("refreshToken", authContext.getRefreshToken());

        SheinCredentialData credentialData = executeSignedPostForData(authContext, normalizeApiPath(authPath), body,
            resolveOpenKeyId(authContext), resolveSecretKey(authContext), new TypeReference<SheinApiResponse<SheinCredentialData>>() {});
        return mergeAuthContext(authContext, credentialData);
    }

    /**
     * 获取店铺基础信息。
     * <p>
     * 若店铺信息接口尚未开通，则退化为授权返回的商家标识信息。
     */
    @Override
    public UnifiedShopInfo getShopInfo(AuthContext authContext) {
        validateMerchantCredentials(authContext);

        try {
            Map<String, Object> payload = executeSignedPost(authContext, normalizeApiPath(shopInfoPath), Collections.emptyMap());
            Map<String, Object> shop = firstMap(payload, "shop", "store", "merchant", "data");
            if (shop != null) {
                return UnifiedShopInfo.builder()
                    .platform(Platform.SHEIN_MX_SELF)
                    .shopId(firstNonBlank(
                        readString(shop, "shopId", "storeId", "merchantId"),
                        authContext.getShopId(),
                        authContext.getMerchantId()))
                    .shopName(firstNonBlank(readString(shop, "shopName", "storeName", "merchantName"), authContext.getShopId()))
                    .countryId(readString(shop, "countryCode", "country"))
                    .shopCode(readString(shop, "shopCode", "storeCode"))
                    .userNickName(readString(shop, "merchantName", "sellerName", "nickName"))
                    .cipher(authContext.getOpenKeyId())
                    .build();
            }
        } catch (Exception e) {
            log.warn("【SHEIN】店铺信息接口调用失败，将退回授权信息组装。原因: {}", e.getMessage());
        }

        return UnifiedShopInfo.builder()
            .platform(Platform.SHEIN_MX_SELF)
            .shopId(firstNonBlank(authContext.getShopId(), authContext.getMerchantId()))
            .shopName(firstNonBlank(authContext.getShopId(), authContext.getMerchantId()))
            .userNickName(authContext.getMerchantId())
            .countryId("MX")
            .cipher(authContext.getOpenKeyId())
            .build();
    }

    private List<Map<String, String>> loadCarrierOptions(AuthContext authContext) {
        try {
            Map<String, Object> payload = executeSignedPost(authContext, normalizeApiPath(carrierListPath), Collections.emptyMap());
            List<Map<String, Object>> carrierList = extractMapList(payload, "carriers", "carrierList", "logisticsProviders", "data");
            if (carrierList.isEmpty()) {
                return Collections.emptyList();
            }

            return carrierList.stream()
                .map(item -> objectMapper.convertValue(item, SheinCarrierInfo.class))
                .filter(item -> StringUtils.hasText(item.getCarrierId()) || StringUtils.hasText(item.getCarrierName()))
                .map(item -> {
                    Map<String, String> carrier = new LinkedHashMap<>();
                    carrier.put("id", firstNonBlank(item.getCarrierId(), item.getCarrierName()));
                    carrier.put("name", firstNonBlank(item.getCarrierName(), item.getCarrierId()));
                    return carrier;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("【SHEIN】查询物流商列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> executeSignedPost(AuthContext authContext, String path, Map<String, Object> body) {
        return executeSignedPostForData(authContext, path, body,
            resolveOpenKeyId(authContext), resolveSecretKey(authContext), new TypeReference<SheinApiResponse<Map<String, Object>>>() {});
    }

    private <T> T executeSignedPostForData(AuthContext authContext,
                                          String path,
                                          Map<String, Object> body,
                                          String openKeyId,
                                          String secretKey,
                                          TypeReference<SheinApiResponse<T>> typeReference) {
        Request request = buildSignedPostRequest(path, body, openKeyId, secretKey, authContext);
        SheinApiResponse<T> response = executeRequest(request, typeReference);
        validateResponse(response, path);
        T payload = firstNonNull(response.getData(), response.getInfo(), response.getResult());
        if (payload == null) {
            throw new EcommIntegrationException("【SHEIN】接口 " + path + " 响应缺少 data/info/result 字段。");
        }
        return payload;
    }

    private Request buildSignedPostRequest(String path,
                                           Map<String, Object> body,
                                           String openKeyId,
                                           String secretKey,
                                           AuthContext authContext) {
        if (!StringUtils.hasText(openKeyId) || !StringUtils.hasText(secretKey)) {
            throw new EcommIntegrationException("【SHEIN】缺少 openKeyId 或 secretKey，无法生成签名。");
        }

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body == null ? Collections.emptyMap() : body);
        } catch (Exception e) {
            throw new EcommIntegrationException("【SHEIN】序列化请求体失败", e);
        }

        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String randomKey = generateRandomKey(5);
        String signature = generateSignature(openKeyId, secretKey, path, timestamp, randomKey);

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(apiBaseUrl + path), "SHEIN API URL invalid");
        Request.Builder builder = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .addHeader("x-lt-openKeyId", openKeyId)
            .addHeader("x-lt-timestamp", timestamp)
            .addHeader("x-lt-signature", signature);

        if (authContext != null && StringUtils.hasText(authContext.getAccessToken())) {
            builder.addHeader("Authorization", "Bearer " + authContext.getAccessToken());
        }
        return builder.build();
    }

    /**
     * SHEIN 签名逻辑：
     * 1. VALUE = openKeyId + "&" + timestamp + "&" + path
     * 2. KEY = secretKey + randomKey
     * 3. HMAC-SHA256 结果先转 hex，再对 hex 字符串做 Base64
     * 4. 最终签名 = randomKey + base64(hexString)
     */
    private String generateSignature(String openKeyId, String secretKey, String path, String timestamp, String randomKey) {
        String value = openKeyId + "&" + timestamp + "&" + path;
        String key = secretKey + randomKey;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String hex = bytesToHex(digest);
            String base64 = Base64.getEncoder().encodeToString(hex.getBytes(StandardCharsets.UTF_8));
            return randomKey + base64;
        } catch (Exception e) {
            throw new EcommIntegrationException("【SHEIN】生成签名失败", e);
        }
    }

    private AuthContext mapToAuthContext(SheinCredentialData credentialData) {
        if (credentialData == null) {
            throw new EcommIntegrationException("【SHEIN】授权响应为空。");
        }
        if (!StringUtils.hasText(credentialData.getOpenKeyId()) || !StringUtils.hasText(credentialData.getSecretKey())) {
            throw new EcommIntegrationException("【SHEIN】授权结果缺少 openKeyId 或 secretKey，请确认应用后台是否返回了解密后的 SecretKey。");
        }

        Instant now = Instant.now();
        return AuthContext.builder()
            .platform(Platform.SHEIN_MX_SELF)
            .accessToken(credentialData.getAccessToken())
            .refreshToken(credentialData.getRefreshToken())
            .accessTokenExpiresAt(resolveExpireTime(now, credentialData.getAccessTokenExpireIn()))
            .refreshTokenExpiresAt(resolveExpireTime(now, credentialData.getRefreshTokenExpireIn()))
            .shopId(firstNonBlank(credentialData.getShopId(), credentialData.getMerchantId()))
            .sellerId(credentialData.getMerchantId())
            .merchantId(credentialData.getMerchantId())
            .openKeyId(credentialData.getOpenKeyId())
            .secretKey(credentialData.getSecretKey())
            .build();
    }

    private AuthContext mergeAuthContext(AuthContext previous, SheinCredentialData credentialData) {
        AuthContext refreshed = mapToAuthContext(credentialData);
        if (!StringUtils.hasText(refreshed.getShopId())) {
            refreshed.setShopId(previous.getShopId());
        }
        if (!StringUtils.hasText(refreshed.getMerchantId())) {
            refreshed.setMerchantId(previous.getMerchantId());
        }
        if (!StringUtils.hasText(refreshed.getSellerId())) {
            refreshed.setSellerId(previous.getSellerId());
        }
        if (!StringUtils.hasText(refreshed.getAccessToken())) {
            refreshed.setAccessToken(previous.getAccessToken());
            refreshed.setAccessTokenExpiresAt(previous.getAccessTokenExpiresAt());
        }
        if (!StringUtils.hasText(refreshed.getRefreshToken())) {
            refreshed.setRefreshToken(previous.getRefreshToken());
            refreshed.setRefreshTokenExpiresAt(previous.getRefreshTokenExpiresAt());
        }
        return refreshed;
    }

    private UnifiedOrder mapToUnifiedOrder(Map<String, Object> orderMap) {
        UnifiedOrder order = new UnifiedOrder();
        String orderStatus = readString(orderMap, "orderStatus", "status");
        order.setOrderId(readString(orderMap, "orderId", "orderNo"));
        order.setOriginalStatus(orderStatus);
        order.setUnifiedStatus(SheinStatusMapper.toUnifiedStatus(orderStatus));
        order.setCreateTime(readInstant(orderMap, "createTime", "orderCreateTime", "createTimeMs"));
        order.setUpdateTime(readInstant(orderMap, "updateTime", "modifiedTime", "updateTimeMs"));
        order.setCurrency(firstNonBlank(readString(orderMap, "currency", "currencyCode"), "MXN"));
        order.setTotalAmount(readBigDecimal(orderMap, "orderAmount", "payAmount", "totalAmount"));
        order.setBuyerInfo(readString(orderMap, "buyerName", "customerName", "buyerId"));

        List<Map<String, Object>> itemMaps = extractMapList(orderMap, "orderItems", "itemList", "skuList", "products");
        List<UnifiedOrderItem> items = itemMaps.stream()
            .map(this::mapToOrderItem)
            .collect(Collectors.toList());
        order.setOrderItems(items);

        UnifiedShipment shipment = new UnifiedShipment();
        shipment.setTrackingNumber(readString(orderMap, "trackingNumber", "trackingNo"));
        shipment.setCarrier(readString(orderMap, "carrierName", "logisticsProviderName"));
        shipment.setShipmentId(readString(orderMap, "shipmentId", "deliveryId", "packageId"));
        shipment.setOriginalStatus(readString(orderMap, "shipmentStatus", "logisticsStatus"));
        shipment.setUnifiedStatus(readString(orderMap, "shipmentStatus", "logisticsStatus"));
        shipment.setShippingAddress(mapToAddress(firstMap(orderMap, "address", "receiverAddress", "shippingAddress")));
        order.setShipment(shipment);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("original_order", orderMap);
        order.setRawData(rawData);
        return order;
    }

    private UnifiedOrderItem mapToOrderItem(Map<String, Object> itemMap) {
        UnifiedOrderItem item = new UnifiedOrderItem();
        item.setOrderLineId(readString(itemMap, "orderLineId", "itemId", "detailId"));
        item.setProductId(readString(itemMap, "productId", "goodsId", "spuId"));
        item.setProductName(readString(itemMap, "productName", "goodsName", "productTitle"));
        item.setSkuId(readString(itemMap, "skuId", "sellerSku", "merchantSku"));
        item.setSkuName(readString(itemMap, "skuName", "specification", "skuAttr"));
        item.setImageUrl(readString(itemMap, "imageUrl", "mainImage", "skuImage"));
        item.setQuantity(readInteger(itemMap, "quantity", "buyCount", "itemQty"));
        item.setUnitPrice(readBigDecimal(itemMap, "unitPrice", "salePrice", "price"));
        return item;
    }

    private UnifiedAddress mapToAddress(Map<String, Object> addressMap) {
        if (addressMap == null || addressMap.isEmpty()) {
            return null;
        }

        UnifiedAddress address = new UnifiedAddress();
        address.setFullName(readString(addressMap, "name", "fullName", "receiverName"));
        address.setPhone(readString(addressMap, "phone", "mobile", "receiverPhone"));
        address.setCountryCode(readString(addressMap, "countryCode", "country"));
        address.setProvince(readString(addressMap, "province", "state"));
        address.setCity(readString(addressMap, "city"));
        address.setDistrict(readString(addressMap, "district", "county", "town"));
        address.setStreet(firstNonBlank(
            readString(addressMap, "addressLine1", "street"),
            joinAddressLines(addressMap, "addressLine1", "addressLine2", "addressLine3")));
        address.setZipCode(readString(addressMap, "zipCode", "postalCode"));
        return address;
    }

    private UnifiedTrackingEvent mapToTrackingEvent(Map<String, Object> eventMap) {
        if (eventMap == null || eventMap.isEmpty()) {
            return null;
        }
        UnifiedTrackingEvent event = new UnifiedTrackingEvent();
        event.setDescription(firstNonBlank(
            readString(eventMap, "description", "desc", "trackDesc"),
            readString(eventMap, "status", "event")));
        event.setLocation(readString(eventMap, "location", "city", "siteName"));
        event.setTime(readInstant(eventMap, "time", "eventTime", "operateTime", "gmtCreate"));
        return event;
    }

    private void validateMerchantCredentials(AuthContext authContext) {
        if (authContext == null) {
            throw new EcommIntegrationException("【SHEIN】AuthContext 不能为空。");
        }
        if (!StringUtils.hasText(resolveOpenKeyId(authContext)) || !StringUtils.hasText(resolveSecretKey(authContext))) {
            throw new EcommIntegrationException("【SHEIN】AuthContext 中缺少 openKeyId 或 secretKey，请持久化授权返回的商家凭证。");
        }
    }

    private void validateResponse(SheinApiResponse<?> response, String path) {
        if (response == null) {
            throw new EcommIntegrationException("【SHEIN】接口 " + path + " 响应为空。");
        }

        String message = firstNonBlank(response.getMsg(), response.getMessage(), response.getErrorMsg());
        if (Boolean.FALSE.equals(response.getSuccess())) {
            throw new EcommIntegrationException("【SHEIN】接口 " + path + " 调用失败: " + message);
        }

        Object code = response.getCode();
        if (code == null) {
            return;
        }

        String codeText = String.valueOf(code).trim();
        if ("0".equals(codeText) || "200".equals(codeText) || "SUCCESS".equalsIgnoreCase(codeText) || "true".equalsIgnoreCase(codeText)) {
            return;
        }

        throw new EcommIntegrationException("【SHEIN】接口 " + path + " 调用失败，code=" + codeText + (StringUtils.hasText(message) ? "，msg=" + message : ""));
    }

    private List<Map<String, Object>> extractOrderList(Map<String, Object> payload) {
        return extractMapList(payload, "orderList", "orders", "list", "data");
    }

    private List<Map<String, Object>> extractMapList(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return Collections.emptyList();
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                return list.stream()
                    .filter(Objects::nonNull)
                    .map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}))
                    .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private Map<String, Object> firstMap(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Map) {
                return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            }
        }
        return null;
    }

    private String resolveNextPageToken(Map<String, Object> payload, String currentPageToken, int currentSize, int pageSize) {
        String pageToken = readString(payload, "nextPageToken", "nextCursor");
        if (StringUtils.hasText(pageToken)) {
            return pageToken;
        }

        Integer totalPage = readInteger(payload, "totalPage", "pages");
        Integer currentPage = readInteger(payload, "pageNo", "currentPage");
        if (currentPage != null && totalPage != null && currentPage < totalPage) {
            return String.valueOf(currentPage + 1);
        }

        if (currentSize <= 0 || pageSize <= 0 || currentSize < pageSize) {
            return null;
        }

        int current = parsePageNo(currentPageToken);
        return String.valueOf(current + 1);
    }

    private int parsePageNo(String pageToken) {
        if (!StringUtils.hasText(pageToken)) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(pageToken), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Instant resolveExpireTime(Instant now, Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return now.plusSeconds(seconds);
    }

    private String resolveOpenKeyId(AuthContext authContext) {
        if (authContext == null) {
            return appId;
        }
        return authContext.getOpenKeyId();
    }

    private String resolveSecretKey(AuthContext authContext) {
        if (authContext == null) {
            return appSecret;
        }
        return authContext.getSecretKey();
    }

    private String normalizeApiPath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new EcommIntegrationException("【SHEIN】接口路径不能为空。");
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String trimLeadingSlash(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String generateRandomKey(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM_KEY_ALPHABET.charAt(RANDOM.nextInt(RANDOM_KEY_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            builder.append(String.format("%02x", aByte));
        }
        return builder.toString();
    }

    private String readString(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(Map<String, Object> map, String... keys) {
        String value = readString(map, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer readInteger(Map<String, Object> map, String... keys) {
        String value = readString(map, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant readInstant(Map<String, Object> map, String... keys) {
        String value = readString(map, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            long raw = Long.parseLong(value);
            if (value.length() >= 13) {
                return Instant.ofEpochMilli(raw);
            }
            return Instant.ofEpochSecond(raw);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String joinAddressLines(Map<String, Object> addressMap, String... keys) {
        List<String> parts = new ArrayList<>();
        for (String key : keys) {
            String value = readString(addressMap, key);
            if (StringUtils.hasText(value)) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }
}
