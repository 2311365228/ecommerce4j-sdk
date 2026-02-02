package com.ecommerce4j.api.platform.mercadolibre;

import com.ecommerce4j.api.EcommAuthorizationService;
import com.ecommerce4j.api.EcommFulfillmentService;
import com.ecommerce4j.api.EcommLogisticsService;
import com.ecommerce4j.api.EcommOrderService;
import com.ecommerce4j.api.dto.*;
import com.ecommerce4j.api.enums.FulfillmentType;
import com.ecommerce4j.api.enums.Platform;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.ecommerce4j.api.platform.AbstractAdapter;
import com.ecommerce4j.api.platform.mercadolibre.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service("MERCADO_LIBRE")
public class MercadoLibreAdapter extends AbstractAdapter implements EcommOrderService, EcommFulfillmentService, EcommLogisticsService, EcommAuthorizationService {

    @Value("${mercado.app_id}")
    private String appId;

    @Value("${mercado.client_secret}")
    private String clientSecret;

    @Value("${mercado.redirect_uri}")
    private String redirectUri;

    // Mercado Libre API 基础URL
    private static final String API_BASE_URL = "https://api.mercadolibre.com";

    // Mercado Libre 授权URL (以墨西哥为例，实际应用中可能需要根据国家配置)
    // https://auth.mercadolibre.com/{country_code}/authorization
    private static final String AUTH_BASE_URL = "https://auth.mercadolibre.com.mx/authorization";

    // JSON媒体类型
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    /**
     * 构建并返回 Mercado Libre 授权URL。
     *
     * @param state    授权状态参数，用于防止CSRF攻击
     * @return 授权URL
     */
    @Override
    public String getAuthorizationUrl(String state) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(AUTH_BASE_URL).newBuilder();
        urlBuilder.addQueryParameter("response_type", "code");
        urlBuilder.addQueryParameter("client_id", appId);
        urlBuilder.addQueryParameter("redirect_uri", redirectUri);
        // state 参数是标准的OAuth2实践，Meli会原样返回
        if (StringUtils.hasText(state)) {
            urlBuilder.addQueryParameter("state", state);
        }
        return urlBuilder.build().toString();
    }

    @Override
    public UnifiedShopInfo getShopInfo(AuthContext authContext) {
        String url = API_BASE_URL + "/users/me";
        Request request = this.buildRequest(authContext.getAccessToken(), url, "GET", null);
        MercadoLibreUserInfo userInfo = executeRequest(request, new TypeReference<>() {});
        if (Objects.isNull(userInfo)) {
            return null;
        }
        return UnifiedShopInfo.builder()
            .platform(authContext.getPlatform())
            .userNickName(userInfo.getNickname())
            .shopId(authContext.getSellerId())
            .shopCode("")
            .shopName(userInfo.getCompany().getCorporateName())
            .countryId(userInfo.getCountryId())
            .cipher("")
            .build();
    }

    /**
     * 使用授权码（code）换取访问令牌和刷新令牌。
     *
     * @param code     授权码
     * @return 认证上下文对象，包含令牌信息
     */
    @Override
    public AuthContext exchangeCodeForTokens(String code) {
        RequestBody formBody = new FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", appId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build();

        Request request = new Request.Builder()
            .url(API_BASE_URL + "/oauth/token")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build();

        MercadoLibreTokenData tokenData = executeRequest(request, new TypeReference<>() {
        });
        return mapToAuthContext(tokenData);
    }

    /**
     * 使用刷新令牌（refresh_token）刷新访问令牌。
     *
     * @param authContext 包含刷新令牌的认证上下文
     * @return 新的认证上下文对象，包含新的令牌信息
     */
    @Override
    public AuthContext refreshTokens(AuthContext authContext) {
        if (!StringUtils.hasText(authContext.getRefreshToken())) {
            throw new EcommIntegrationException("【Meli】刷新令牌不可用，无法刷新。");
        }

        RequestBody formBody = new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", appId)
            .add("client_secret", clientSecret)
            .add("refresh_token", authContext.getRefreshToken())
            .build();

        Request request = new Request.Builder()
            .url(API_BASE_URL + "/oauth/token")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build();

        MercadoLibreTokenData tokenData = executeRequest(request, new TypeReference<>() {
        });

        // Meli 在刷新时可能会返回一个新的 refresh_token，也可能不返回。
        // 如果返回了新的，需要用新的覆盖旧的。如果没返回，继续使用旧的。
        String newRefreshToken = StringUtils.hasText(tokenData.getRefreshToken()) ? tokenData.getRefreshToken() : authContext.getRefreshToken();
        tokenData.setRefreshToken(newRefreshToken);

        return mapToAuthContext(tokenData);
    }

    /**
     * 准备订单履约，主要是下载面单。
     *
     * @param authContext 认证上下文
     * @param orderId     订单ID
     * @return 履约操作类型及相关信息
     */
    @Override
    public FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing) {
        // 1. 获取对于的shipmentId
        String shipmentId = resolveShipmentId(authContext, orderId);
        if (!StringUtils.hasText(shipmentId)) {
            throw new EcommIntegrationException("【Meli】订单 " + orderId + " 没有有效的货运信息(shipping_id)。");
        }

        // 2. 获取货运详情以检查状态
        MercadoLibreShipment shipment = internalGetShipmentDetails(authContext, shipmentId);
        if (shipment == null) {
            log.error("【Meli】无法获取订单 {} 对应的货运详情。Shipment ID: {}", orderId, shipmentId);
            return null;
        }

        // 3. 下载面单
        String labelUrl = API_BASE_URL + "/shipment_labels";
        Map<String, Object> params = new HashMap<>();
        params.put("shipment_ids", shipmentId);
        params.put("response_type", "pdf");
        Request labelRequest = buildRequest(authContext.getAccessToken(), labelUrl, "GET", params);
        byte[] labelBytes = executeRequestForBytes(labelRequest);
        if (labelBytes == null || labelBytes.length == 0) {
            log.error("【Meli】下载面单失败，返回内容为空。Shipment ID: " + shipmentId);
        }

        return FulfillmentAction.builder()
            .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
            .trackingNumber(shipment.getId())
            .labelContent(labelBytes)
            .receiverName(Objects.isNull(shipment.getDestination()) ? "" : shipment.getDestination().getReceiverName())
            .labelMimeType("application/pdf") // Meli 通常返回PDF
            .build();
    }

    /**
     * 提交卖家发货的物流追踪信息 (适用于 me1 模式)。
     *
     * @param authContext  认证上下文
     * @param orderId      订单ID
     * @param trackingInfo 追踪信息
     */
    @Override
    public void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo) {
        // 此功能主要用于 Mercado Envíos 1 (me1)，即卖家自发货模式
        String shipmentId = resolveShipmentId(authContext, orderId);
        if (!StringUtils.hasText(shipmentId)) {
            throw new EcommIntegrationException("【Meli】订单 " + orderId + " 没有有效的货运信息，无法提交运单号。");
        }

        // 根据文档，更新运单号和承运商信息通常是对shipment资源进行PUT操作
        String url = API_BASE_URL + "/shipments/" + shipmentId;
        Map<String, Object> body = new HashMap<>();
        body.put("status", "shipped");
        body.put("tracking_number", trackingInfo.getTrackingNumber());
        body.put("tracking_method", trackingInfo.getShippingProviderId());

        Request request = buildRequest(authContext.getAccessToken(), url, "PUT", body);
        // 执行请求并期望一个成功的响应 (e.g., 200 OK)
        executeRequest(request, new TypeReference<Object>() {
        });
        log.info("【Meli】成功为货运 {} 提交运单号 {}", shipmentId, trackingInfo.getTrackingNumber());
    }

    /**
     * 获取指定订单的物流追踪事件。
     *
     * @param authContext 认证上下文
     * @param orderId     订单ID
     * @return 统一包裹信息，包含追踪事件列表
     */
    @Override
    public UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId) {
        String shipmentId = resolveShipmentId(authContext, orderId);
        if (!StringUtils.hasText(shipmentId)) {
            log.warn("【Meli】订单 {} 没有有效的货运信息，无法获取物流轨迹。", orderId);
            return new UnifiedShipment();
        }

        MercadoLibreShipment shipment = internalGetShipmentDetails(authContext, shipmentId);
        if (shipment == null) {
            return new UnifiedShipment();
        }

        UnifiedShipment unifiedShipment = new UnifiedShipment();
        unifiedShipment.setTrackingNumber(shipment.getTrackingNumber());
        unifiedShipment.setCarrier(shipment.getTrackingMethod());

        // 查询货运节点状态信息
        List<UnifiedTrackingEvent> events = new ArrayList<>();
        MercadoLibreShipmentHistoryResponse mercadoLibreShipmentNodes = internalGetShipmentHistory(authContext, shipmentId);
        if (!Objects.isNull(mercadoLibreShipmentNodes)) {
            MercadoLibreShipmentHistoryResponse.DateHistory dateHistory = mercadoLibreShipmentNodes.getDateHistory();
            addEventIfPresent(events, dateHistory.getDateCreated(), "Order created, package is being prepared");
            addEventIfPresent(events, dateHistory.getDateFirstPrinted(), "Shipping label printed by seller");
            addEventIfPresent(events, dateHistory.getDateHandling(), "Package handling at fulfillment center");
            addEventIfPresent(events, dateHistory.getDateReadyToShip(), "Package ready to be shipped");
            addEventIfPresent(events, dateHistory.getDateShipped(), "Package shipped, leaving fulfillment center");
            addEventIfPresent(events, dateHistory.getDateDelivered(), "Package delivered successfully");
            addEventIfPresent(events, dateHistory.getDateNotDelivered(), "Package delivery attempt failed");
            addEventIfPresent(events, dateHistory.getDateCancelled(), "Shipment has been cancelled");
            addEventIfPresent(events, dateHistory.getDateReturned(), "Package has been returned");
            // 按时间升序排序
            events.sort(Comparator.comparing(UnifiedTrackingEvent::getTime));
        }
        unifiedShipment.setTrackingEvents(events);
        return unifiedShipment;
    }

    /**
     * 辅助方法，用于解析日期字符串并创建物流事件对象。
     *
     * @param events      物流事件列表
     * @param dateString  日期时间字符串 (ISO 8601格式)
     * @param description 事件描述
     */
    private void addEventIfPresent(List<UnifiedTrackingEvent> events, String dateString, String description) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                // Mercado Libre 返回的是带时区偏移的ISO 8601格式，可以直接解析为Instant
                Instant time = Instant.parse(dateString);
                UnifiedTrackingEvent event = new UnifiedTrackingEvent();
                event.setDescription(description);
                event.setTime(time);
                // 注意：该API响应中不包含地点信息，因此location字段为null
                event.setLocation(null);
                events.add(event);
            } catch (Exception e) {
                log.error("无法解析日期: {}, 错误: {}" ,dateString, e.getMessage());
            }
        }
    }

    /**
     * 获取订单列表。
     *
     * @param authContext 认证上下文
     * @param query       订单查询参数
     * @return 统一订单列表
     */
    @Override
    public PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query) {

        // 分页参数: Meli使用offset/limit，我们需要从pageToken(这里用作offset)和pageSize转换
        int limit = query.getPageSize() > 0 ? query.getPageSize() : 50;
        int offset = Objects.isNull(query.getPageToken()) ? 0 : Integer.parseInt(query.getPageToken());
        if (limit > 51) {
            throw new EcommIntegrationException("【Mercado Libre】查询订单列表，分页大小不能超过51");
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller", authContext.getSellerId());
        queryParams.put("limit", limit);
        queryParams.put("offset", offset);
        queryParams.put("sort", "date_desc");

        if (query.getCreateTimeFrom() != null) {
            Instant truncatedFrom = query.getCreateTimeFrom().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
            queryParams.put("order.date_created.from", DateTimeFormatter.ISO_INSTANT.format(truncatedFrom));
        }
        if (query.getCreateTimeTo() != null) {
            Instant truncatedTo = query.getCreateTimeTo().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
            queryParams.put("order.date_created.to", DateTimeFormatter.ISO_INSTANT.format(truncatedTo));
        }
        if (StringUtils.hasText(query.getOrderStatus())) {
            queryParams.put("order.status", query.getOrderStatus());
        }
        String url = API_BASE_URL + "/orders/search";
        Request request = buildRequest(authContext.getAccessToken(), url, "GET", queryParams);

        MercadoLibreOrderSearchResponse response = executeRequest(request, new TypeReference<>() {});

        if (response == null || CollectionUtils.isEmpty(response.getResults())) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        List<MercadoLibreOrder> results = response.getResults();
        if (query.isFilterFullStock()) {
            // 过滤掉full仓的订单
            results.removeIf(item -> {
                if (CollectionUtils.isEmpty(item.getOrderItems())) {
                    return false;
                }
                MercadoLibreOrder.OrderItem firstItem = item.getOrderItems().get(0);
                return firstItem != null && firstItem.getStock() != null;
            });
        }

        // 合单处理（如果有packId，则已packId进行合单）
        List<UnifiedOrder> unifiedOrders = new ArrayList<>();
        Set<String> processedPackIds = new HashSet<>(); // 用于记录本批次已处理过的packId
        for (MercadoLibreOrder rawOrder : results) {
            // 检查是否有 Pack ID
            String packId = rawOrder.getPackId();
            if (StringUtils.hasText(packId)) {
                // 如果是合单，且本批次还没处理过这个包
                if (!processedPackIds.contains(packId)) {
                    try {
                        // 调用 Pack 接口获取包内所有 Order ID
                        List<String> packOrderIds = internalGetOrderIdsInPack(authContext, packId);
                        // 获取包内所有子订单的详细信息
                        List<UnifiedOrder> childOrders = getOrderDetails(authContext, packOrderIds);
                        if (!CollectionUtils.isEmpty(childOrders)) {
                            // 将多个子订单合并为一个 UnifiedOrder
                            UnifiedOrder combinedOrder = mergePackToUnifiedOrder(packId, childOrders);
                            unifiedOrders.add(combinedOrder);
                        }

                        // 标记该 Pack 已处理
                        processedPackIds.add(packId);
                    } catch (Exception e) {
                        log.info("【Meli】处理合单失败，PackID: {}", packId, e);
                        // 降级处理：如果合单逻辑失败，尝试按单品处理当前单
                        unifiedOrders.add(mapToUnifiedOrder(authContext, rawOrder));
                    }
                } else {
                    // 如果这个 packId 已经处理过了（比如列表里先有了 Order A 属于 Pack 1，现在是 Order B 也属于 Pack 1），直接跳过
                    log.debug("【Meli】PackID {} 已在当前批次处理过，跳过子订单 {}", packId, rawOrder.getId());
                }
            } else {
                // 没有 Pack ID，按普通单处理
                unifiedOrders.add(mapToUnifiedOrder(authContext, rawOrder));
            }
        }

        // 从所有订单中收集不重复的商品ID
        Set<String> itemIds = unifiedOrders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .map(UnifiedOrderItem::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (!itemIds.isEmpty()) {
            Map<String, MercadoLibreItem> itemDetails = this.getItemDetails(authContext, itemIds);

            // 填充商品图片到原订单
            unifiedOrders.forEach(order ->
                order.getOrderItems().forEach(item -> {
                    MercadoLibreItem mercadoLibreItem = itemDetails.get(item.getProductId());
                    if (Objects.nonNull(mercadoLibreItem) && !mercadoLibreItem.getPictures().isEmpty()) {
                        item.setImageUrl(mercadoLibreItem.getPictures().get(0).getSecureUrl()); // 只填充图片列表中的第一张图片
                    }
                })
            );
        }

        // 计算下一页的pageToken (offset)
        String nextPageToken = null;
        if (response.getPaging() != null) {
            int currentOffset = response.getPaging().getOffset();
            int total = response.getPaging().getTotal();
            int currentLimit = response.getPaging().getLimit();
            if ((currentOffset + currentLimit) < total) {
                nextPageToken = String.valueOf(currentOffset + currentLimit);
            }
        }

        return new PaginatedResult<>(unifiedOrders, nextPageToken);
    }

    /**
     * 批量获取 Mercado Libre 商品信息
     * 使用 Multiget API，每批最多20个ID。
     *
     * @param authContext 认证上下文
     * @param itemIds     去重后的商品ID集合
     * @return 一个从商品ID到商品详细信息的映射
     */
    private Map<String, MercadoLibreItem> getItemDetails(AuthContext authContext, Set<String> itemIds) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }

        final int BATCH_SIZE = 20; // 每次最多查询20个
        List<String> itemIdList = new ArrayList<>(itemIds);
        Map<String, MercadoLibreItem> itemDetailMap = new ConcurrentHashMap<>();

        // 将ID列表分割成多个批次进行处理
        for (int i = 0; i < itemIdList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, itemIdList.size());
            List<String> batchIds = itemIdList.subList(i, end);

            String idsParam = String.join(",", batchIds);
            String url = API_BASE_URL + "/items?ids=" + idsParam;
            Map<String, Object> queryParams = Collections.singletonMap("ids", idsParam);

            Request request = buildRequest(authContext.getAccessToken(), url, "GET", queryParams);

            try {
                List<MercadoLibreItemMultigetResponse> responses = executeRequest(request, new TypeReference<>() {
                });
                responses.stream()
                    // 确保请求成功且body和pictures字段存在
                    .filter(resp -> resp.getCode() == 200 && resp.getBody() != null && !CollectionUtils.isEmpty(resp.getBody().getPictures()))
                    .forEach(resp -> {
                        MercadoLibreItem item = resp.getBody();
                        itemDetailMap.put(item.getId(), item);
                    });
            } catch (Exception e) {
                log.error("【Meli】批量获取商品信息失败，IDs: {}", idsParam, e);
                // 单个批次失败不应中断整个流程，仅记录日志
            }
        }
        return itemDetailMap;
    }


    /**
     * 获取指定订单ID的详细信息。
     *
     * @param authContext 认证上下文
     * @param orderIds    订单ID列表
     * @return 统一订单列表
     */
    @Override
    public List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }

        List<UnifiedOrder> resultList = new ArrayList<>();

        for (String id : orderIds) {
            // 1. 尝试作为普通订单获取
            MercadoLibreOrder order = null;
            try {
                order = internalGetOrderDetails(authContext, id);
            } catch (Exception ignored) {

            }
            if (order != null) {
                // 成功获取到普通订单
                resultList.add(mapToUnifiedOrder(authContext, order));
            } else {
                // 当前的id请求订单详情找不到，那么有可能是packId，尝试作为packId获取
                try {
                    List<String> childIds = internalGetOrderIdsInPack(authContext, id);
                    if (!CollectionUtils.isEmpty(childIds)) {
                        // 递归获取子订单详情
                        List<UnifiedOrder> childUnifiedOrders = childIds.parallelStream() // 并行流
                            .map(childId -> {
                                try {
                                    return internalGetOrderDetails(authContext, childId);
                                } catch (Exception e) {
                                    log.error("【Meli】并发获取子单详情失败 ID: {}", childId, e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .map(o -> mapToUnifiedOrder(authContext, o))
                            .collect(Collectors.toList());
                        // 合并为 Pack Order
                        UnifiedOrder packOrder = mergePackToUnifiedOrder(id, childUnifiedOrders);
                        if (packOrder != null) {
                            resultList.add(packOrder);
                        }
                    }
                } catch (Exception e) {
                    log.warn("【Meli】ID {} 既不是有效订单也不是有效 Pack", id);
                }
            }

        }
        return resultList;
    }

    /**
     * 获取单个订单的完整详情。
     */
    private MercadoLibreOrder internalGetOrderDetails(AuthContext authContext, String orderId) {
        String url = API_BASE_URL + "/orders/" + orderId;
        Request request = buildRequest(authContext.getAccessToken(), url, "GET", null);
        try {
            return executeRequest(request, new TypeReference<>() {
            });
        } catch (EcommIntegrationException e) {
            // 如果订单未找到 (404)，返回null而不是抛出异常
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("【Meli】获取订单详情失败，订单ID不存在: {}", orderId);
                return null;
            }
            throw e;
        }
    }

    /**
     * 获取单个货运的完整详情。
     */
    private MercadoLibreShipment internalGetShipmentDetails(AuthContext authContext, String shipmentId) {
        String url = API_BASE_URL + "/shipments/" + shipmentId;
        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + authContext.getAccessToken())
            // x-format-new 是获取新版货运格式的强制要求
            .addHeader("x-format-new", "true")
            .build();
        try {
            return executeRequest(request, new TypeReference<>() {
            });
        } catch (EcommIntegrationException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("【Meli】获取货运详情失败，货运ID不存在: {}", shipmentId);
                return null;
            }
            throw e;
        }
    }

    /**
     * 获取物流节点信息
     * @param authContext
     * @param shipmentId
     * @return
     */
    private MercadoLibreShipmentHistoryResponse internalGetShipmentHistory(AuthContext authContext, String shipmentId) {
        String url = API_BASE_URL + "/shipments/" + shipmentId + "/history";
        Request request = buildRequest(authContext.getAccessToken(), url, "GET", null);
        return executeRequest(request, new TypeReference<>() {});
    }


    /**
     * 构建一个通用的API请求。
     *
     * @param accessToken 访问令牌
     * @param url         API路径 (例如, baseurl/orders/search)
     * @param method      HTTP方法 ("GET", "POST", "PUT")
     * @param params      请求参数。对于GET，会作为URL查询参数；对于POST/PUT，会序列化为JSON请求体。
     * @return 构建好的 OkHttp Request 对象
     */
    private Request buildRequest(String accessToken, String url, String method, Map<String, Object> params) {
        // 1. 构建基础URL
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder requestBuilder = new Request.Builder()
            .addHeader("Authorization", "Bearer " + accessToken);

        RequestBody body = null;

        // 2. 根据HTTP方法处理参数
        if ("GET".equalsIgnoreCase(method)) {
            if (params != null && !params.isEmpty()) {
                params.forEach((key, value) -> urlBuilder.addQueryParameter(key, String.valueOf(value)));
            }
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            if (params != null && !params.isEmpty()) {
                try {
                    String jsonBody = objectMapper.writeValueAsString(params);
                    body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
                } catch (Exception e) {
                    throw new EcommIntegrationException("【Meli】序列化请求体失败", e);
                }
            } else {
                // 对于POST/PUT，即使没有参数，也需要一个空的请求体
                body = RequestBody.create(new byte[0]);
            }
        }

        // 3. 设置URL和HTTP方法
        requestBuilder.url(urlBuilder.build());
        switch (method.toUpperCase()) {
            case "POST":
                requestBuilder.post(body);
                break;
            case "PUT":
                requestBuilder.put(body);
                break;
            case "GET":
            default:
                requestBuilder.get();
                break;
        }

        return requestBuilder.build();
    }

    /**
     * 将 Mercado Libre 订单对象映射为我们系统内部的统一订单对象。
     */
    private UnifiedOrder mapToUnifiedOrder(AuthContext authContext, MercadoLibreOrder meliOrder) {
        UnifiedOrder unifiedOrder = new UnifiedOrder();

        unifiedOrder.setOriginalStatus(meliOrder.getStatus());
        unifiedOrder.setUnifiedStatus(MercadoLibreStatusMapper.toUnifiedStatus(meliOrder.getStatus()));

        unifiedOrder.setOrderId(String.valueOf(meliOrder.getId()));
        if (StringUtils.hasText(meliOrder.getDateCreated())) {
            unifiedOrder.setCreateTime(Instant.parse(meliOrder.getDateCreated()));
        }
        if (StringUtils.hasText(meliOrder.getDateClosed())) {
            unifiedOrder.setUpdateTime(Instant.parse(meliOrder.getDateClosed()));
        }

        // 支付信息
        if (!CollectionUtils.isEmpty(meliOrder.getPayments())) {
            MercadoLibreOrder.Payment payment = meliOrder.getPayments().get(0);
            unifiedOrder.setCurrency(payment.getCurrencyId());
            unifiedOrder.setTotalAmount(payment.getTransactionAmount());
        }

        // 买家信息
        if (meliOrder.getBuyer() != null) {
            unifiedOrder.setBuyerInfo(meliOrder.getBuyer().getFirstName() + " " + meliOrder.getBuyer().getLastName());
        }

        // 订单行项目
        if (!CollectionUtils.isEmpty(meliOrder.getOrderItems())) {
            List<UnifiedOrderItem> items = meliOrder.getOrderItems().stream().map(li -> {
                UnifiedOrderItem item = new UnifiedOrderItem();
                item.setOrderLineId(li.getItem().getId()); // Meli没有单独的订单行ID，用item ID代替
                item.setProductId(li.getItem().getId());
                item.setProductName(li.getItem().getTitle());
                item.setSkuId(li.getItem().getSellerSku());
                item.setSkuName(li.getItem().getSellerSku()); // Meli的SKU就是seller_sku
                item.setQuantity(li.getQuantity());
                item.setUnitPrice(li.getUnitPrice());
                // Meli API通常不直接在订单中返回图片，需要额外查询item
                item.setImageUrl(null);
                return item;
            }).collect(Collectors.toList());
            unifiedOrder.setOrderItems(items);
        }

        // 发货信息
        UnifiedShipment shipment = new UnifiedShipment();
        if (meliOrder.getShipping() != null && meliOrder.getShipping().getId() != null) {
            String shippingId = meliOrder.getShipping().getId();
            // 关键：存储shipping_id，用于后续履约操作
            shipment.setShipmentId(shippingId);

            if (meliOrder.getShipping().getReceiverAddress() != null) {
                MercadoLibreOrder.ReceiverAddress meliAddress = meliOrder.getShipping().getReceiverAddress();
                UnifiedAddress address = new UnifiedAddress();
                address.setFullName(meliAddress.getReceiverName());
                address.setPhone(meliAddress.getReceiverPhone());
                // Meli将街道、城市、州等分开，这里合并处理
                String fullAddress = String.join(", ",
                    meliAddress.getAddressLine(),
                    meliAddress.getCity() != null ? meliAddress.getCity().getName() : "",
                    meliAddress.getState() != null ? meliAddress.getState().getName() : ""
                );
                address.setStreet(fullAddress);
                address.setZipCode(meliAddress.getZipCode());
                address.setCountryCode(meliAddress.getCountry() != null ? meliAddress.getCountry().getId() : null);
                shipment.setShippingAddress(address);
            }

            // 注：Mercado查询订单返回的状态有两种，在order中的status仅表示付款完成前的状态
            // 查询物流状态信息，如果有物流信息的状态，则最终以物流信息上的状态为最终的订单状态
            MercadoLibreShipment mercadoLibreShipment = internalGetShipmentDetails(authContext, shippingId);
            if (Objects.nonNull(mercadoLibreShipment) && StringUtils.hasText(mercadoLibreShipment.getStatus())) {
                unifiedOrder.setOriginalStatus(mercadoLibreShipment.getStatus());
                unifiedOrder.setUnifiedStatus(MercadoLibreStatusMapper.toUnifiedStatus(mercadoLibreShipment.getStatus()));
            }
        }
        unifiedOrder.setShipment(shipment);

        // 存储原始对象
        unifiedOrder.setRawData(Collections.singletonMap("original_order", meliOrder));
        return unifiedOrder;
    }

    /**
     * 将 Mercado Libre 的授权令牌数据映射为我们系统内部的认证上下文。
     */
    private AuthContext mapToAuthContext(MercadoLibreTokenData tokenData) {
        return AuthContext.builder()
            .platform(Platform.MERCADO_LIBRE)
            .accessToken(tokenData.getAccessToken())
            .refreshToken(tokenData.getRefreshToken())
            .accessTokenExpiresAt(Instant.now().plusSeconds(tokenData.getExpiresIn()))
            // Meli的refresh token有效期很长（6个月），这里不设置具体过期时间，依赖刷新逻辑
            .refreshTokenExpiresAt(null)
            // 在Meli中, user_id 就是卖家ID
            .sellerId(String.valueOf(tokenData.getUserId()))
            .shopId(String.valueOf(tokenData.getUserId())) // shopId也使用userId
            .build();
    }

    /**
     * 调用 Pack API 获取合单内的所有 Order ID
     */
    private List<String> internalGetOrderIdsInPack(AuthContext authContext, String packId) {
        String url = API_BASE_URL + "/packs/" + packId;

        // 将 params 传入 buildRequest (原来是 null)
        Request request = buildRequest(authContext.getAccessToken(), url, "GET", null);
        // 使用 Map 接收响应，避免创建太多 DTO
        Map<String, Object> response = executeRequest(request, new TypeReference<>() {});

        if (response != null && response.containsKey("orders")) {
            List<Map<String, Object>> ordersList = (List<Map<String, Object>>) response.get("orders");
            return ordersList.stream()
                .map(o -> String.valueOf(o.get("id")))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 将多个子订单合并为一个 UnifiedOrder (以 PackID 为主键)
     */
    private UnifiedOrder mergePackToUnifiedOrder(String packId, List<UnifiedOrder> childOrders) {
        if (CollectionUtils.isEmpty(childOrders)) return null;

        // 1. 选取第一个子订单作为"主模版"，复制买家信息、地址等（因为合单的买家和地址是一样的）
        UnifiedOrder template = childOrders.get(0);

        UnifiedOrder packOrder = new UnifiedOrder();
        // 使用 PackID 作为 OMS 的 OrderID
        packOrder.setOrderId(packId);

        // 复制通用信息
        packOrder.setBuyerInfo(template.getBuyerInfo());
        packOrder.setCreateTime(template.getCreateTime());
        packOrder.setUpdateTime(template.getUpdateTime());
        packOrder.setCurrency(template.getCurrency());
        packOrder.setShipment(template.getShipment()); // 合单共享同一个 Shipment ID

        // 状态逻辑：通常取第一个单的状态，或者判断如果所有单都 Cancelled 才是 Cancelled
        packOrder.setOriginalStatus(template.getOriginalStatus());
        packOrder.setUnifiedStatus(template.getUnifiedStatus());

        // 2. 聚合金额和商品明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<UnifiedOrderItem> allItems = new ArrayList<>();

        for (UnifiedOrder child : childOrders) {
            if (child.getTotalAmount() != null) {
                totalAmount = totalAmount.add(child.getTotalAmount());
            }
            if (!CollectionUtils.isEmpty(child.getOrderItems())) {
                allItems.addAll(child.getOrderItems());
            }
        }

        packOrder.setTotalAmount(totalAmount);
        packOrder.setOrderItems(allItems);

        // 标记为合单 (可选，如果在 UnifiedOrder 有扩展字段的话)
        // packOrder.addExtension("is_pack", "true");
        // packOrder.addExtension("child_order_ids", childOrders.stream().map(UnifiedOrder::getOrderId).collect(Collectors.joining(",")));

        return packOrder;
    }

    /**
     * 解析 Shipment ID
     * 该方法会自动判断传入的 ID 是普通 Order ID 还是 Pack ID。
     * 上层业务无需关心 ID 类型，由此方法在底层进行"双模探测"。
     *
     * @param authContext 认证上下文
     * @param orderOrPackId 可能是订单ID，也可能是PackID
     * @return 对应的 Shipment ID or Null
     */
    private String resolveShipmentId(AuthContext authContext, String orderOrPackId) {
        // --- 尝试 1: 当作普通订单查询 ---
        // 大多数情况可能还是普通订单，或者我们先假设它是普通订单
        MercadoLibreOrder order = null;
        try {
            order = internalGetOrderDetails(authContext, orderOrPackId);
        } catch (Exception ignored) {

        }

        if (order != null) {
            // 命中！这确实是一个普通订单ID
            log.debug("【Meli】ID {} 识别为普通订单，ShipmentId: {}", orderOrPackId, order.getShipping().getId());
            return Objects.isNull(order.getShipping()) ? null : order.getShipping().getId();
        }

        // --- 尝试 2: 当作 Pack (合单) 查询 ---
        // 查询订单返回null，说明这可能是一个 Pack ID
        log.debug("【Meli】ID {} 不是普通订单，尝试识别为 Pack...", orderOrPackId);

        try {
            // 调用 Pack 接口 (复用之前提到的获取 Pack 详情逻辑，或者这里单独写个简单的)
            String packUrl = API_BASE_URL + "/packs/" + orderOrPackId;
            Request request = buildRequest(authContext.getAccessToken(), packUrl, "GET", null);

            // 解析 Pack 响应
            Map<String, Object> packResponse = executeRequest(request, new TypeReference<>() {});

            if (packResponse != null && packResponse.containsKey("shipment")) {
                Map<String, Object> shipmentObj = (Map<String, Object>) packResponse.get("shipment");
                if (shipmentObj != null && shipmentObj.containsKey("id")) {
                    String shipmentId = String.valueOf(shipmentObj.get("id"));
                    log.info("【Meli】ID {} 成功识别为合单(Pack)，ShipmentId: {}", orderOrPackId, shipmentId);
                    return shipmentId;
                }
            }
        } catch (Exception e) {
            // 如果 Pack 接口也报错，那说明这个 ID 既不是 Order 也不是 Pack，或者 Token 过期等
            log.warn("【Meli】ID {} 识别 Pack 失败: {}", orderOrPackId, e.getMessage());
        }

        return null;
    }
}
