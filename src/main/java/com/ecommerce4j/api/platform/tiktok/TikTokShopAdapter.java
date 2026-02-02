package com.ecommerce4j.api.platform.tiktok;

import com.ecommerce4j.api.EcommAuthorizationService;
import com.ecommerce4j.api.EcommFulfillmentService;
import com.ecommerce4j.api.EcommLogisticsService;
import com.ecommerce4j.api.EcommOrderService;
import com.ecommerce4j.api.dto.*;
import com.ecommerce4j.api.enums.FulfillmentType;
import com.ecommerce4j.api.enums.Platform;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.ecommerce4j.api.platform.AbstractAdapter;
import com.ecommerce4j.api.platform.tiktok.dto.*;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TikTok电商适配器，用于与TikTok Shop开放平台进行交互。
 * 实现了订单、履约、物流和授权等核心服务接口。
 */
@Slf4j
@Service("TIKTOK_SHOP")
public class TikTokShopAdapter extends AbstractAdapter implements EcommOrderService, EcommFulfillmentService, EcommLogisticsService, EcommAuthorizationService {

    @Value("${tiktok.app_key}")
    private String appKey;

    @Value("${tiktok.app_secret}")
    private String appSecret;

    @Value("${tiktok.auth_url}")
    private String authUrl;


    // TikTok API基础URL
    private static final String API_BASE_URL = "https://open-api.tiktokglobalshop.com";

    private static final String AUTH_BASE_URL = "https://auth.tiktok-shops.com";
    // HMAC签名算法
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    // JSON媒体类型
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    // --- EcommAuthorizationService 授权服务 ---

    /**
     * 构建并返回TikTok授权URL。
     *
     * @param state 授权状态参数，用于防止CSRF攻击
     * @return 授权URL
     */
    @Override
    public String getAuthorizationUrl(String state) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(authUrl).newBuilder();
        urlBuilder.addQueryParameter("state", state);
        // 通常，权限范围(scopes)是可配置的
        // urlBuilder.addQueryParameter("scope", "order.read,fulfillment.write");
        return urlBuilder.build().toString();
    }

    @Override
    public UnifiedShopInfo getShopInfo(AuthContext authContext) {
        String path = "/authorization/202309/shops";
        Request request = buildSignedRequestNoShopCipher("GET", path, authContext, null, null);
        TikTokApiResponse<TikTokShopInfo> response = executeRequest(request, new TypeReference<>() {
        });
        TikTokShopInfo tikTokShopInfo = response.getData();
        if (Objects.isNull(tikTokShopInfo) || CollectionUtils.isEmpty(tikTokShopInfo.getShops())) {
            return null;
        }
        // 虽然接口返回的shops是一个集合，但是目前业务场景没有一账号多店铺的情况，只返回第一个店铺
        TikTokShopInfo.Shop shop = tikTokShopInfo.getShops().get(0);
        return UnifiedShopInfo.builder()
            .platform(authContext.getPlatform())
            .userNickName("")
            .shopId(shop.getId())
            .shopName(shop.getName())
            .countryId(shop.getRegion())
            .cipher(shop.getCipher())
            .shopCode(shop.getCode())
            .sellerType(shop.getSellerType())
            .build();
    }

    /**
     * 使用授权码（code）换取访问令牌和刷新令牌。
     *
     * @param code 授权码
     * @return 认证上下文对象，包含令牌信息
     */
    @Override
    public AuthContext exchangeCodeForTokens(String code) {
        String path = "/api/v2/token/get";
        String url = AUTH_BASE_URL + path + "?app_key=" + appKey + "&app_secret=" + appSecret +
            "&auth_code=" + code + "&grant_type=authorized_code";

        // 构建GET请求
        Request request = new Request.Builder().url(url).get().build();
        // 执行请求并解析响应
        TikTokApiResponse<TikTokTokenData> response = executeRequest(request, new TypeReference<>() {});
        validateResponse(response);
        // 将TikTok的令牌数据映射为我们系统的认证上下文对象
        return mapToAuthContext(response.getData());
    }

    /**
     * 使用刷新令牌（refresh_token）刷新访问令牌。
     *
     * @param authContext 包含刷新令牌的认证上下文
     * @return 新的认证上下文对象，包含新的令牌信息
     */
    @Override
    public AuthContext refreshTokens(AuthContext authContext) {
        String path = "/api/v2/token/refresh";
        String url = AUTH_BASE_URL + path + "?app_key=" + appKey + "&app_secret=" + appSecret +
            "&refresh_token=" + authContext.getRefreshToken() + "&grant_type=refresh_token";

        Request request = new Request.Builder().url(url).get().build();
        TikTokApiResponse<TikTokTokenData> response = executeRequest(request, new TypeReference<>() {});
        validateResponse(response);
        return mapToAuthContext(response.getData());
    }

    // --- EcommOrderService 订单服务 ---

    /**
     * 获取订单列表。
     *
     * @param authContext 认证上下文
     * @param query 订单查询参数
     * @return 统一订单列表
     */
    @Override
    public PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query) {
        String path = "/order/202309/orders/search";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page_size", String.valueOf(query.getPageSize()));
        if (StringUtils.hasText(query.getPageToken())) {
            queryParams.put("page_token", query.getPageToken());
        }

         queryParams.put("sort_field", "create_time");
         queryParams.put("sort_order", "ASC");

        // -- b. 放在 Request Body 中的参数 --
        Map<String, Object> bodyMap = new HashMap<>();
        if (query.getCreateTimeFrom() != null) {
            // curl示例中使用 _ge (greater than or equal) 和 _lt (less than)
            bodyMap.put("create_time_ge", query.getCreateTimeFrom().getEpochSecond());
        }
        if (query.getCreateTimeTo() != null) {
            bodyMap.put("create_time_lt", query.getCreateTimeTo().getEpochSecond());
        }
        if (StringUtils.hasText(query.getOrderStatus())) {
            bodyMap.put("order_status", query.getOrderStatus());
        }

        String requestBody;
        try {
            // 如果bodyMap为空，则请求体为空JSON对象 "{}"
            requestBody = objectMapper.writeValueAsString(bodyMap);
        } catch (Exception e) {
            throw new EcommIntegrationException("【TikTok】序列化订单查询请求体失败", e);
        }

        Request request = buildSignedRequest("POST", path, authContext, queryParams, requestBody);

        TikTokApiResponse<Map<String, Object>> response = executeRequest(request, new TypeReference<TikTokApiResponse<Map<String, Object>>>() {});
        validateResponse(response);

        Map<String, Object> data = response.getData();
        if (data == null || CollectionUtils.isEmpty((List<?>) data.get("orders"))) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderList = (List<Map<String, Object>>) data.get("orders");

        List<UnifiedOrder> unifiedOrders = orderList.stream()
            .map(orderMap -> {
                TikTokOrder tikTokOrder = objectMapper.convertValue(orderMap, TikTokOrder.class);
                return mapToUnifiedOrder(tikTokOrder);
            })
            .collect(Collectors.toList());

        // 从响应中提取 next_page_token 用于下一次请求
        String nextPageToken = (String) data.get("next_page_token");

        return new PaginatedResult<>(unifiedOrders, nextPageToken);
    }

    /**
     * 获取指定订单ID的详细信息。
     *
     * @param authContext 认证上下文
     * @param orderIds 订单ID列表
     * @return 统一订单列表
     */
    @Override
    public List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds) {
        String path = "/order/202309/orders";
        Map<String, String> queryParams = new HashMap<>();
        // 将订单ID列表拼接成逗号分隔的字符串
        queryParams.put("ids", String.join(",", orderIds));

        Request request = buildSignedRequest("GET", path, authContext, queryParams, null);
        TikTokApiResponse<Map<String, Object>> response = executeRequest(request, new TypeReference<TikTokApiResponse<Map<String, Object>>>() {});
        validateResponse(response);

        List<Map<String, Object>> orderList = (List<Map<String, Object>>) response.getData().get("orders");
        if(CollectionUtils.isEmpty(orderList)) {
            return Collections.emptyList();
        }

        return orderList.stream()
            .map(orderMap -> {
                TikTokOrder tikTokOrder = objectMapper.convertValue(orderMap, TikTokOrder.class);
                return mapToUnifiedOrder(tikTokOrder);
            })
            .collect(Collectors.toList());
    }

    // --- EcommFulfillmentService 履约服务 ---

    /**
     * 准备订单履约，根据发货类型决定后续操作（下载面单或提供物流信息）。
     *
     * @param authContext 认证上下文
     * @param orderId 订单ID
     * @param autoShipIfMissing true: 若待发货且无面单，自动尝试调用发货接口; false: 仅获取面单，不执行发货动作
     * @return 履约操作类型及相关信息
     */
    @Override
    public FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing) {
        // 1. 获取订单详情以确定发货类型
        List<UnifiedOrder> orders = getOrderDetails(authContext, Collections.singletonList(orderId));
        if (CollectionUtils.isEmpty(orders)) {
            throw new EcommIntegrationException("未找到订单: " + orderId);
        }
        Map<String, Object> rawData = orders.get(0).getRawData();
        TikTokOrder rawOrder = (TikTokOrder) rawData.get("original_order");
        List<TikTokOrder.Package> packages = rawOrder.getPackages();

        if (CollectionUtils.isEmpty(packages)) {
            throw new EcommIntegrationException("订单 " + orderId + " 尚未打包，无法获取面单。");
        }

        // 查找第一个需要平台发货的包裹
        TikTokOrder.Package tiktokShippingPackage = packages.stream()
            .findFirst()
            .orElse(null);

        String path = String.format("/fulfillment/202309/packages/%s/shipping_documents", tiktokShippingPackage.getId());
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("document_type", "SHIPPING_LABEL");
        queryParams.put("document_size", "A6");
        queryParams.put("document_format", "PDF");

        Request request = buildSignedRequest("GET", path, authContext, queryParams, null);
        TikTokApiResponse<TikTokShippingDocumentResponse> response = executeRequest(request, new TypeReference<>() {});

        if (Objects.isNull(response.getData()) || !StringUtils.hasText(response.getData().getDocUrl())) {
            if (autoShipIfMissing && "AWAITING_SHIPMENT".equals(rawOrder.getStatus())) {
                // 待发货，但是没有拿到面单，尝试调用发货接口，然后再拉取订单
                String shipPackagePath = String.format("/fulfillment/202309/packages/%s/ship", tiktokShippingPackage.getId());
                Request shipPackageRequest = buildSignedRequest("POST", shipPackagePath, authContext, null, null);
                TikTokApiResponse<?> shipPackageRes = executeRequest(shipPackageRequest, new TypeReference<>() {});
                if (Integer.valueOf(0).equals(shipPackageRes.getCode())) {
                    // 再次尝试调用获取面单接口
                    return this.prepareFulfillment(authContext, orderId, true);
                }
            }
            throw new EcommIntegrationException("未查询到面单文件，订单id:"+ orderId);
        }

        String docUrl = response.getData().getDocUrl();
        Request fileUrlRequest = new Request
            .Builder()
            .url(docUrl)
            .build();
        byte[] labelBytes = executeRequestForBytes(fileUrlRequest);

        return FulfillmentAction.builder()
            .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
            .trackingNumber(response.getData().getTrackingNumber())
            // 处理收货人名称
            .receiverName(Optional.ofNullable(orders.get(0))
                .map(UnifiedOrder::getShipment)
                .map(UnifiedShipment::getShippingAddress)
                .map(UnifiedAddress::getFullName)
                .orElse(""))
            .labelContent(labelBytes)
            .labelMimeType("application/pdf") // PDF格式
            .build();
    }

    /**
     * 提交卖家发货的物流追踪信息。
     *
     * @param authContext 认证上下文
     * @param orderId 订单ID
     * @param trackingInfo 追踪信息
     */
    @Override
    public void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo) {
        // 对于“卖家发货”，需要提交包裹的追踪信息。
        // 首先，从订单详情中获取包裹ID。
        List<UnifiedOrder> orders = getOrderDetails(authContext, Collections.singletonList(orderId));
        if (CollectionUtils.isEmpty(orders)) {
            throw new EcommIntegrationException("未找到订单以提交追踪号: " + orderId);
        }
        TikTokOrder.Package firstPackage = ((TikTokOrder) orders.get(0).getRawData().get("original_order")).getPackages().get(0);

        String path = String.format("/fulfillment/202309/packages/%s/ship", firstPackage.getId());

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("tracking_number", trackingInfo.getTrackingNumber());
        bodyMap.put("shipping_provider_id", trackingInfo.getShippingProviderId());

        try {
            String body = objectMapper.writeValueAsString(bodyMap);
            Request request = buildSignedRequest("POST", path, authContext, new HashMap<>(), body);
            TikTokApiResponse<Object> response = executeRequest(request, new TypeReference<TikTokApiResponse<Object>>() {});
            validateResponse(response);
            log.info("成功为包裹 {} 提交追踪号", firstPackage.getId());
        } catch (Exception e) {
            throw new EcommIntegrationException("序列化追踪信息请求体失败", e);
        }
    }

    // --- EcommLogisticsService 物流服务 ---

    /**
     * 获取指定订单的物流追踪事件。
     *
     * @param authContext 认证上下文
     * @param orderId 订单ID
     * @return 统一包裹信息，包含追踪事件列表
     */
    @Override
    public UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId) {
        String path = String.format("/fulfillment/202309/orders/%s/tracking", orderId);
        Request request = buildSignedRequest("GET", path, authContext, new HashMap<>(), null);
        TikTokApiResponse<Map<String, Object>> response = executeRequest(request, new TypeReference<TikTokApiResponse<Map<String, Object>>>() {});
        validateResponse(response);

        // 从响应中提取追踪历史列表
        List<Map<String, Object>> trackingEventsMap = (List<Map<String, Object>>) response.getData().get("tracking");

        UnifiedShipment shipment = new UnifiedShipment();
        if(!CollectionUtils.isEmpty(trackingEventsMap)) {
            // 将TikTok的追踪事件映射为我们系统的统一追踪事件
            List<UnifiedTrackingEvent> events = trackingEventsMap.stream().map(eventMap -> {
                UnifiedTrackingEvent event = new UnifiedTrackingEvent();
                event.setDescription((String) eventMap.get("description"));
                event.setLocation((String) eventMap.get("location"));
                // TikTok API返回的时间戳是字符串，需要先转换为Long
                event.setTime(Instant.ofEpochMilli((Long) eventMap.get("update_time_millis")));
                return event;
            }).collect(Collectors.toList());
            shipment.setTrackingEvents(events);
        }
        return shipment;
    }


    // --- Private Helper Methods 私有辅助方法 ---

    /**
     * 构建带签名的HTTP请求, 不需要ShopCipher, 通常用于获取店铺信息。
     *
     * @param method HTTP方法（GET/POST）
     * @param path API路径
     * @param authContext 认证上下文
     * @param queryParams 请求查询参数
     * @param body 请求体字符串
     * @return 签名后的Request对象
     */
    private Request buildSignedRequestNoShopCipher(String method, String path, AuthContext authContext, Map<String, String> queryParams, String body) {
        // 1. 添加通用参数到 Query String
        Map<String, String> finalQueryParams = new HashMap<>();
        if (queryParams != null && !queryParams.isEmpty()) {
            finalQueryParams.putAll(queryParams);
        }
        finalQueryParams.put("app_key", this.appKey);
        finalQueryParams.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        // 2. 生成签名。签名内容包括了请求体
        String sign = generateSign(path, finalQueryParams, body);
        finalQueryParams.put("sign", sign);

        // 3. 构建包含所有 Query 参数的 URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_BASE_URL + path).newBuilder();
        finalQueryParams.forEach(urlBuilder::addQueryParameter);

        // 4. 构建 Request
        Request.Builder requestBuilder = new Request.Builder()
            .url(urlBuilder.build())
            .addHeader("x-tts-access-token", authContext.getAccessToken())
            .addHeader("Content-Type", "application/json");

        if ("POST".equalsIgnoreCase(method)) {
            // 确保 POST 请求总是带有请求体（即使是空字符串）
            requestBuilder.post(RequestBody.create(body != null ? body : "", JSON_MEDIA_TYPE));
        } else {
            requestBuilder.get();
        }

        return requestBuilder.build();
    }

    /**
     * 构建带签名的HTTP请求。
     *
     * @param method HTTP方法（GET/POST）
     * @param path API路径
     * @param authContext 认证上下文
     * @param queryParams 请求查询参数
     * @param body 请求体字符串
     * @return 签名后的Request对象
     */
    private Request buildSignedRequest(String method, String path, AuthContext authContext, Map<String, String> queryParams, String body) {
        // 1. 添加通用参数到 Query String
        Map<String, String> finalQueryParams = new HashMap<>();
        if (queryParams != null && !queryParams.isEmpty()) {
            finalQueryParams.putAll(queryParams);
        }
        finalQueryParams.put("app_key", this.appKey);
        finalQueryParams.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        if (authContext.getShopCipher() == null) {
            throw new EcommIntegrationException("AuthContext 中缺少 shop_cipher，无法调用 TikTok API。");
        }
        finalQueryParams.put("shop_cipher", authContext.getShopCipher());

        // 2. 生成签名。签名内容包括了请求体
        String sign = generateSign(path, finalQueryParams, body);
        finalQueryParams.put("sign", sign);

        // 3. 构建包含所有 Query 参数的 URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_BASE_URL + path).newBuilder();
        finalQueryParams.forEach(urlBuilder::addQueryParameter);

        // 4. 构建 Request
        Request.Builder requestBuilder = new Request.Builder()
            .url(urlBuilder.build())
            .addHeader("x-tts-access-token", authContext.getAccessToken())
            .addHeader("Content-Type", "application/json");

        if ("POST".equalsIgnoreCase(method)) {
            // 确保 POST 请求总是带有请求体（即使是空字符串）
            requestBuilder.post(RequestBody.create(body != null ? body : "", JSON_MEDIA_TYPE));
        } else {
            requestBuilder.get();
        }

        return requestBuilder.build();
    }

    /**
     * 生成TikTok API请求签名。
     *
     * @param path API路径
     * @param params 请求参数
     * @param body 请求体
     * @return HMAC-SHA256签名字符串
     */
    private String generateSign(String path, Map<String, String> params, String body) {
        // 使用TreeMap按键名排序参数
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder baseString = new StringBuilder();
        baseString.append(path);
        // 拼接path和所有参数键值对
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            baseString.append(entry.getKey()).append(entry.getValue());
        }
        // 如果有请求体，也拼接进去
        if (body != null) {
            baseString.append(body);
        }
        // 构造最终的签名字符串：appSecret + baseString + appSecret
        String stringToSign = this.appSecret + baseString + this.appSecret;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new EcommIntegrationException("生成TikTok API签名失败", e);
        }
    }

    /**
     * 验证TikTok API响应是否成功。
     *
     * @param response TikTok API响应对象
     * @throws EcommIntegrationException 如果响应码不为0
     */
    private void validateResponse(TikTokApiResponse<?> response) {
        if (response.getCode() != 0) {
            log.error("TikTok API错误。代码: {}, 信息: {}, 请求ID: {}", response.getCode(), response.getMessage(), response.getRequestId());
            throw new EcommIntegrationException(String.format("TikTok API错误: %s (代码: %d)", response.getMessage(), response.getCode()));
        }
    }

    /**
     * 将TikTok订单对象映射为我们系统内部的统一订单对象。
     *
     * @param tikTokOrder TikTok订单对象
     * @return 统一订单对象
     */
    private UnifiedOrder mapToUnifiedOrder(TikTokOrder tikTokOrder) {
        UnifiedOrder unifiedOrder = new UnifiedOrder();
        unifiedOrder.setOrderId(tikTokOrder.getId());
        unifiedOrder.setOriginalStatus(tikTokOrder.getStatus());
        // 使用TikTokStatusMapper将状态标准化
        unifiedOrder.setUnifiedStatus(TikTokStatusMapper.toUnifiedStatus(tikTokOrder.getStatus()));
        unifiedOrder.setCreateTime(Instant.ofEpochSecond(tikTokOrder.getCreateTime()));
        unifiedOrder.setUpdateTime(Instant.ofEpochSecond(tikTokOrder.getUpdateTime()));

        if (tikTokOrder.getPayment() != null) {
            unifiedOrder.setCurrency(tikTokOrder.getPayment().getCurrency());
            unifiedOrder.setTotalAmount(new BigDecimal(tikTokOrder.getPayment().getTotalAmount()));
        }

        // 买家信息可以从收件人地址中获取
        if (tikTokOrder.getRecipientAddress() != null) {
            unifiedOrder.setBuyerInfo(tikTokOrder.getRecipientAddress().getName());
        }

        // 映射订单商品行项目
        if (!CollectionUtils.isEmpty(tikTokOrder.getLineItems())) {
            List<UnifiedOrderItem> items = tikTokOrder.getLineItems().stream().map(li -> {
                UnifiedOrderItem item = new UnifiedOrderItem();
                item.setOrderLineId(li.getId());
                item.setProductId(li.getProductId());
                item.setProductName(li.getProductName());
                item.setSkuId(li.getSkuId());

                // 设置最终的skuName,以卖家sku和平台sku作为组合
                String sellerSku = StringUtils.hasText(li.getSellerSku()) ? li.getSellerSku() : "-";
                String skuName = StringUtils.hasText(li.getSkuName()) ? li.getSkuName() : "-";
                item.setSkuName(sellerSku.concat("/").concat(skuName));

                item.setQuantity(li.getQuantity() != null ? li.getQuantity() : 1);
                item.setUnitPrice(new BigDecimal(li.getSalePrice()));
                if (!StringUtils.hasText(item.getImageUrl())) {
                    // 再次尝试找到商品图片
                    item.setImageUrl(li.getSkuImage());
                }
                return item;
            }).collect(Collectors.toList());
            unifiedOrder.setOrderItems(items);
        }

        // 映射发货信息
        UnifiedShipment shipment = new UnifiedShipment();
        shipment.setTrackingNumber(tikTokOrder.getTrackingNumber());
        shipment.setCarrier(tikTokOrder.getShippingProvider());
        if (tikTokOrder.getRecipientAddress() != null) {
            UnifiedAddress address = new UnifiedAddress();
            TikTokOrder.TikTokRecipientAddress tiktokAddress = tikTokOrder.getRecipientAddress();
            address.setFullName(tiktokAddress.getName());
            address.setPhone(tiktokAddress.getPhone());
            address.setStreet(tiktokAddress.getFullAddress());
            address.setZipCode(tiktokAddress.getPostalCode());
            shipment.setShippingAddress(address);
        }
        unifiedOrder.setShipment(shipment);

        // 存储原始对象，以备后用
        unifiedOrder.setRawData(Collections.singletonMap("original_order", tikTokOrder));

        return unifiedOrder;
    }

    /**
     * 将TikTok的授权令牌数据映射为我们系统内部的认证上下文。
     *
     * @param tokenData TikTok授权令牌数据
     * @return 认证上下文对象
     */
    private AuthContext mapToAuthContext(TikTokTokenData tokenData) {

        return AuthContext.builder()
            .platform(Platform.TIKTOK_SHOP)
            .accessToken(tokenData.getAccessToken())
            .refreshToken(tokenData.getRefreshToken())
            .accessTokenExpiresAt(Instant.ofEpochSecond(tokenData.getAccessTokenExpireIn()))
            .refreshTokenExpiresAt(Instant.ofEpochSecond(tokenData.getRefreshTokenExpireIn()))
            .sellerId(tokenData.getOpenId()) // 将open_id存储在sellerId中
            .build();
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param hash 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
