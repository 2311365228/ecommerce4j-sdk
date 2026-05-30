package com.ecommerce4j.api.platform.lazada;

import com.ecommerce4j.api.EcommAuthorizationService;
import com.ecommerce4j.api.EcommFulfillmentService;
import com.ecommerce4j.api.EcommLogisticsService;
import com.ecommerce4j.api.EcommOrderService;
import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.FulfillmentAction;
import com.ecommerce4j.api.dto.FulfillmentDocument;
import com.ecommerce4j.api.dto.FulfillmentPackRequest;
import com.ecommerce4j.api.dto.FulfillmentPackageResult;
import com.ecommerce4j.api.dto.FulfillmentProviderOption;
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
import com.ecommerce4j.api.platform.lazada.dto.LazadaAuthModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaFulfillmentModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaOrderModels;
import com.ecommerce4j.api.platform.lazada.dto.LazadaStatusMapper;
import com.ecommerce4j.api.platform.lazada.dto.LazadaTraceModels;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lazada 平台适配器
 */
@Service("LAZADA")
public class LazadaAdapter extends AbstractAdapter implements EcommOrderService, EcommFulfillmentService, EcommLogisticsService, EcommAuthorizationService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_ORDER_IDS = 50;

    @Value("${lazada.app_key}")
    private String appKey;

    @Value("${lazada.app_secret}")
    private String appSecret;

    @Value("${lazada.redirect_uri}")
    private String redirectUri;

    @Value("${lazada.authorize_url:https://auth.lazada.com/oauth/authorize}")
    private String authorizeUrl;

    @Value("${lazada.auth_base_url:https://auth.lazada.com/rest}")
    private String authBaseUrl;

    private LazadaApiClient lazadaApiClient;

    @PostConstruct
    void initClient() {
        this.lazadaApiClient = new LazadaApiClient(httpClient, downloadClient, objectMapper, appKey, appSecret, authBaseUrl);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        StringBuilder builder = new StringBuilder(authorizeUrl)
            .append("?response_type=code")
            .append("&force_auth=true")
            .append("&redirect_uri=")
            .append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
            .append("&client_id=")
            .append(URLEncoder.encode(appKey, StandardCharsets.UTF_8));
        if (StringUtils.hasText(state)) {
            builder.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    @Override
    public AuthContext exchangeCodeForTokens(String code) {
        if (!StringUtils.hasText(code)) {
            throw new EcommIntegrationException("【Lazada】授权 code 不能为空。");
        }
        LazadaAuthModels.TokenResponse response = lazadaApiClient.exchangeCodeForTokens(code);
        return mapToAuthContext(response);
    }

    @Override
    public AuthContext refreshTokens(AuthContext authContext) {
        if (authContext == null || !StringUtils.hasText(authContext.getRefreshToken())) {
            throw new EcommIntegrationException("【Lazada】refreshToken 不能为空。");
        }
        LazadaAuthModels.TokenResponse response = lazadaApiClient.refreshTokens(authContext.getRefreshToken());
        AuthContext refreshed = mapToAuthContext(response);
        refreshed.setSellerId(StringUtils.hasText(refreshed.getSellerId()) ? refreshed.getSellerId() : authContext.getSellerId());
        refreshed.setShopId(StringUtils.hasText(refreshed.getShopId()) ? refreshed.getShopId() : authContext.getShopId());
        refreshed.setSiteCountry(StringUtils.hasText(refreshed.getSiteCountry()) ? refreshed.getSiteCountry() : authContext.getSiteCountry());
        refreshed.setAccountId(StringUtils.hasText(refreshed.getAccountId()) ? refreshed.getAccountId() : authContext.getAccountId());
        refreshed.setAccountName(StringUtils.hasText(refreshed.getAccountName()) ? refreshed.getAccountName() : authContext.getAccountName());
        return refreshed;
    }

    @Override
    public UnifiedShopInfo getShopInfo(AuthContext authContext) {
        LazadaAuthModels.SellerResponse response = lazadaApiClient.getSeller(authContext);
        LazadaAuthModels.Seller seller = response.getData();
        if (seller == null) {
            return null;
        }
        return UnifiedShopInfo.builder()
            .platform(Platform.LAZADA)
            .userNickName(authContext.getAccountName())
            .shopId(seller.getSellerId())
            .shopName(seller.getName())
            .countryId(authContext.getSiteCountry())
            .cipher("")
            .shopCode(seller.getShortCode())
            .sellerType(seller.getCb() == null ? null : seller.getCb().toString())
            .build();
    }

    @Override
    public PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query) {
        if (query == null) {
            throw new EcommIntegrationException("【Lazada】OrderQuery 不能为空。");
        }
        if (query.getUpdateTimeFrom() == null && query.getCreateTimeFrom() == null) {
            throw new EcommIntegrationException("【Lazada】updateTimeFrom 或 createTimeFrom 至少需要传一个。");
        }

        int offset = 0;
        if (StringUtils.hasText(query.getPageToken())) {
            try {
                offset = Integer.parseInt(query.getPageToken());
            } catch (NumberFormatException e) {
                throw new EcommIntegrationException("【Lazada】pageToken 必须是数字 offset。", e);
            }
        }

        int pageSize = query.getPageSize() > 0 ? Math.min(query.getPageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("offset", String.valueOf(offset));
        queryParameters.put("limit", String.valueOf(pageSize));
        queryParameters.put("sort_by", "updated_at");
        queryParameters.put("sort_direction", "ASC");
        if (query.getUpdateTimeFrom() != null) {
            queryParameters.put("update_after", query.getUpdateTimeFrom().toString());
        }
        if (query.getUpdateTimeTo() != null) {
            queryParameters.put("update_before", query.getUpdateTimeTo().toString());
        }
        if (query.getCreateTimeFrom() != null) {
            queryParameters.put("created_after", query.getCreateTimeFrom().toString());
        }
        if (query.getCreateTimeTo() != null) {
            queryParameters.put("created_before", query.getCreateTimeTo().toString());
        }
        if (StringUtils.hasText(query.getOrderStatus())) {
            queryParameters.put("status", query.getOrderStatus());
        }

        LazadaOrderModels.OrdersResponse response = lazadaApiClient.getOrders(authContext, queryParameters);
        LazadaOrderModels.OrdersData data = response.getData();
        if (data == null || CollectionUtils.isEmpty(data.getOrders())) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        List<LazadaOrderModels.Order> headers = data.getOrders();
        Map<String, List<LazadaOrderModels.OrderItem>> orderItemsByOrderId = loadOrderItemsByOrderIds(authContext,
            headers.stream().map(LazadaOrderModels.Order::getOrderId).collect(Collectors.toList()));

        List<UnifiedOrder> orders = headers.stream()
            .map(order -> mapToUnifiedOrder(order, orderItemsByOrderId.get(order.getOrderId())))
            .collect(Collectors.toList());

        String nextPageToken = null;
        if (data.getCountTotal() != null && offset + headers.size() < data.getCountTotal()) {
            nextPageToken = String.valueOf(offset + headers.size());
        } else if (headers.size() == pageSize) {
            nextPageToken = String.valueOf(offset + headers.size());
        }
        return new PaginatedResult<>(orders, nextPageToken);
    }

    @Override
    public List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }

        Map<String, List<LazadaOrderModels.OrderItem>> orderItemsByOrderId = loadOrderItemsByOrderIds(authContext, orderIds);
        List<UnifiedOrder> result = new ArrayList<>();
        for (String orderId : orderIds) {
            LazadaOrderModels.OrderResponse orderResponse = lazadaApiClient.getOrder(authContext, orderId);
            if (orderResponse.getData() != null) {
                result.add(mapToUnifiedOrder(orderResponse.getData(), orderItemsByOrderId.get(orderId)));
            }
        }
        return result;
    }

    @Override
    public FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing) {
        LazadaOrderModels.OrderItemsResponse orderItemsResponse = lazadaApiClient.getOrderItems(authContext, orderId);
        List<LazadaOrderModels.OrderItem> orderItems = orderItemsResponse.getData();
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new EcommIntegrationException("【Lazada】未找到订单行，orderId=" + orderId);
        }
        validateSellerManagedOrder(orderId, orderItems);
        LazadaOrderModels.Order order = lazadaApiClient.getOrder(authContext, orderId).getData();
        if (order == null) {
            throw new EcommIntegrationException("【Lazada】未找到订单头，orderId=" + orderId);
        }
        UnifiedAddress shippingAddress = resolveShippingAddress(order);

        LinkedHashSet<String> packageIds = orderItems.stream()
            .map(LazadaOrderModels.OrderItem::getPackageId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (packageIds.size() == 1) {
            FulfillmentDocument document = getPackageDocument(authContext, packageIds.iterator().next());
            String receiverName = buildReceiverName(shippingAddress);
            String trackingNumber = resolveSingleTrackingNumber(orderItems);
            return FulfillmentAction.builder()
                .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
                .trackingNumber(trackingNumber)
                .labelContent(document.getContent())
                .receiverName(receiverName)
                .labelMimeType(document.getMimeType())
                .build();
        }

        if (!autoShipIfMissing) {
            return FulfillmentAction.builder()
                .fulfillmentType(FulfillmentType.ERROR)
                .errorMessage("【Lazada】请先调用 getShipmentProviders -> packOrderItems -> getPackageDocument 显式履约。")
                .build();
        }

        List<String> orderLineIds = orderItems.stream()
            .map(LazadaOrderModels.OrderItem::getOrderItemId)
            .collect(Collectors.toList());
        List<FulfillmentProviderOption> providerOptions = getShipmentProviders(authContext, orderId, orderLineIds);
        if (providerOptions.size() != 1) {
            return FulfillmentAction.builder()
                .fulfillmentType(FulfillmentType.ERROR)
                .errorMessage("【Lazada】当前订单无法自动选择唯一履约参数，请先显式调用 getShipmentProviders。")
                .build();
        }

        FulfillmentProviderOption providerOption = providerOptions.get(0);
        FulfillmentPackRequest packRequest = FulfillmentPackRequest.builder()
            .orderId(orderId)
            .orderLineIds(orderLineIds)
            .deliveryType("dropship")
            .shippingAllocateType(providerOption.getShippingAllocateType())
            .shipmentProviderCode(providerOption.getShipmentProviderCode())
            .build();

        List<FulfillmentPackageResult> packResults = packOrderItems(authContext, packRequest);
        LinkedHashSet<String> packedPackageIds = packResults.stream()
            .map(FulfillmentPackageResult::getPackageId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (packedPackageIds.size() != 1) {
            return FulfillmentAction.builder()
                .fulfillmentType(FulfillmentType.ERROR)
                .errorMessage("【Lazada】自动打包后返回了多个包裹，请改用显式履约接口处理。")
                .build();
        }

        FulfillmentDocument document = getPackageDocument(authContext, packedPackageIds.iterator().next());
        String receiverName = buildReceiverName(shippingAddress);
        String trackingNumber = packResults.get(0).getTrackingNumber();
        return FulfillmentAction.builder()
            .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
            .trackingNumber(trackingNumber)
            .labelContent(document.getContent())
            .receiverName(receiverName)
            .labelMimeType(document.getMimeType())
            .build();
    }

    @Override
    public void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo) {
        throw new EcommIntegrationException("【Lazada】请使用显式履约接口，当前平台不支持 submitTracking。");
    }

    @Override
    public List<FulfillmentProviderOption> getShipmentProviders(AuthContext authContext, String orderId, List<String> orderLineIds) {
        if (!StringUtils.hasText(orderId) || CollectionUtils.isEmpty(orderLineIds)) {
            throw new EcommIntegrationException("【Lazada】查询履约参数时，orderId 和 orderLineIds 都不能为空。");
        }

        LazadaOrderModels.OrderItemsResponse orderItemsResponse = lazadaApiClient.getOrderItems(authContext, orderId);
        List<LazadaOrderModels.OrderItem> orderItems = orderItemsResponse.getData();
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new EcommIntegrationException("【Lazada】未找到订单行，orderId=" + orderId);
        }
        validateSellerManagedOrder(orderId, orderItems);

        LazadaFulfillmentModels.ShipmentProviderOrder shipmentProviderOrder = new LazadaFulfillmentModels.ShipmentProviderOrder();
        shipmentProviderOrder.setOrderId(orderId);
        shipmentProviderOrder.setOrderItemIds(orderLineIds);

        LazadaFulfillmentModels.ShipmentProvidersRequest request = new LazadaFulfillmentModels.ShipmentProvidersRequest();
        request.setOrders(Collections.singletonList(shipmentProviderOrder));

        LazadaFulfillmentModels.ShipmentProvidersResponse response = lazadaApiClient.getShipmentProviders(authContext, request);
        validateOperationResult(response.getResult(), "GetShipmentProviders");

        LazadaFulfillmentModels.ShipmentProvidersData data = response.getResult().getData();
        if (data == null) {
            return Collections.emptyList();
        }
        if (Objects.equals(data.getPlatformDefault(), 1)) {
            FulfillmentProviderOption option = FulfillmentProviderOption.builder()
                .shipmentProviderCode(null)
                .shipmentProviderName(null)
                .shippingAllocateType(data.getShippingAllocateType())
                .platformDefault(true)
                .build();
            return Collections.singletonList(option);
        }
        if (CollectionUtils.isEmpty(data.getShipmentProviders())) {
            return Collections.emptyList();
        }
        return data.getShipmentProviders().stream()
            .map(provider -> FulfillmentProviderOption.builder()
                .shipmentProviderCode(provider.getProviderCode())
                .shipmentProviderName(provider.getName())
                .shippingAllocateType(data.getShippingAllocateType())
                .platformDefault(false)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<FulfillmentPackageResult> packOrderItems(AuthContext authContext, FulfillmentPackRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId()) || CollectionUtils.isEmpty(request.getOrderLineIds())) {
            throw new EcommIntegrationException("【Lazada】打包请求缺少 orderId 或 orderLineIds。");
        }
        if (!StringUtils.hasText(request.getShippingAllocateType())) {
            throw new EcommIntegrationException("【Lazada】shippingAllocateType 不能为空，请先调用 getShipmentProviders。");
        }

        LazadaOrderModels.OrderItemsResponse orderItemsResponse = lazadaApiClient.getOrderItems(authContext, request.getOrderId());
        List<LazadaOrderModels.OrderItem> orderItems = orderItemsResponse.getData();
        if (CollectionUtils.isEmpty(orderItems)) {
            throw new EcommIntegrationException("【Lazada】未找到订单行，orderId=" + request.getOrderId());
        }
        validateSellerManagedOrder(request.getOrderId(), orderItems);

        Map<String, LazadaOrderModels.OrderItem> orderItemMap = orderItems.stream()
            .collect(Collectors.toMap(LazadaOrderModels.OrderItem::getOrderItemId, item -> item, (left, right) -> left));

        for (String orderLineId : request.getOrderLineIds()) {
            LazadaOrderModels.OrderItem orderItem = orderItemMap.get(orderLineId);
            if (orderItem == null) {
                throw new EcommIntegrationException("【Lazada】订单行不存在，orderItemId=" + orderLineId);
            }
            String status = orderItem.getStatus();
            if (!"pending".equalsIgnoreCase(status) && !"repacked".equalsIgnoreCase(status)) {
                throw new EcommIntegrationException("【Lazada】订单行状态不允许 Pack，orderItemId=" + orderLineId + "，status=" + status);
            }
        }

        LazadaFulfillmentModels.PackOrder packOrder = new LazadaFulfillmentModels.PackOrder();
        packOrder.setOrderId(request.getOrderId());
        packOrder.setOrderItemList(request.getOrderLineIds());

        LazadaFulfillmentModels.PackRequest packRequest = new LazadaFulfillmentModels.PackRequest();
        packRequest.setPackOrderList(Collections.singletonList(packOrder));
        packRequest.setDeliveryType(StringUtils.hasText(request.getDeliveryType()) ? request.getDeliveryType() : "dropship");
        packRequest.setShippingAllocateType(request.getShippingAllocateType());
        packRequest.setShipmentProviderCode(request.getShipmentProviderCode());

        LazadaFulfillmentModels.PackResponse response = lazadaApiClient.pack(authContext, packRequest);
        validateOperationResult(response.getResult(), "Pack");

        LazadaFulfillmentModels.PackData data = response.getResult().getData();
        if (data == null || CollectionUtils.isEmpty(data.getPackOrderList())) {
            return Collections.emptyList();
        }

        List<FulfillmentPackageResult> results = new ArrayList<>();
        for (LazadaFulfillmentModels.PackOrderResult orderResult : data.getPackOrderList()) {
            if (CollectionUtils.isEmpty(orderResult.getOrderItemList())) {
                continue;
            }
            for (LazadaFulfillmentModels.PackItemResult itemResult : orderResult.getOrderItemList()) {
                if (!"0".equals(itemResult.getItemErrCode())) {
                    throw new EcommIntegrationException("【Lazada】Pack 失败，orderItemId=" + itemResult.getOrderItemId() + "，msg=" + itemResult.getMsg());
                }
                FulfillmentPackageResult result = FulfillmentPackageResult.builder()
                    .orderId(orderResult.getOrderId())
                    .orderLineIds(Collections.singletonList(itemResult.getOrderItemId()))
                    .packageId(itemResult.getPackageId())
                    .trackingNumber(itemResult.getTrackingNumber())
                    .shipmentProviderName(itemResult.getShipmentProvider())
                    .shipmentProviderCode(request.getShipmentProviderCode())
                    .shippingAllocateType(request.getShippingAllocateType())
                    .retryable(Boolean.TRUE.equals(itemResult.getRetry()))
                    .message(itemResult.getMsg())
                    .build();
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public FulfillmentDocument getPackageDocument(AuthContext authContext, String packageId) {
        if (!StringUtils.hasText(packageId)) {
            throw new EcommIntegrationException("【Lazada】packageId 不能为空。");
        }

        LazadaFulfillmentModels.PackageRef packageRef = new LazadaFulfillmentModels.PackageRef();
        packageRef.setPackageId(packageId);
        LazadaFulfillmentModels.PackageDocumentRequest request = new LazadaFulfillmentModels.PackageDocumentRequest();
        request.setDocType("PDF");
        request.setPackages(Collections.singletonList(packageRef));
        request.setPrintItemList(Boolean.FALSE);

        LazadaFulfillmentModels.PackageDocumentResponse response = lazadaApiClient.getPackageDocument(authContext, request);
        validateOperationResult(response.getResult(), "GetPackageDocument");

        LazadaFulfillmentModels.PackageDocumentData data = response.getResult().getData();
        if (data == null) {
            throw new EcommIntegrationException("【Lazada】面单响应缺少 data。");
        }
        if (StringUtils.hasText(data.getPdfUrl())) {
            return FulfillmentDocument.builder()
                .packageId(packageId)
                .mimeType("application/pdf")
                .content(lazadaApiClient.downloadPdf(data.getPdfUrl()))
                .build();
        }
        if (StringUtils.hasText(data.getFile())) {
            return FulfillmentDocument.builder()
                .packageId(packageId)
                .mimeType("application/pdf")
                .content(lazadaApiClient.decodeDocumentFile(data.getFile()))
                .build();
        }
        throw new EcommIntegrationException("【Lazada】面单响应既没有 pdf_url，也没有 file 内容。");
    }

    @Override
    public void readyToShip(AuthContext authContext, String packageId) {
        if (!StringUtils.hasText(packageId)) {
            throw new EcommIntegrationException("【Lazada】packageId 不能为空。");
        }

        LazadaFulfillmentModels.PackageRef packageRef = new LazadaFulfillmentModels.PackageRef();
        packageRef.setPackageId(packageId);
        LazadaFulfillmentModels.ReadyToShipRequest request = new LazadaFulfillmentModels.ReadyToShipRequest();
        request.setPackages(Collections.singletonList(packageRef));

        LazadaFulfillmentModels.ReadyToShipResponse response = lazadaApiClient.readyToShip(authContext, request);
        validateOperationResult(response.getResult(), "ReadyToShip");
        LazadaFulfillmentModels.ReadyToShipData data = response.getResult().getData();
        if (data == null || CollectionUtils.isEmpty(data.getPackages())) {
            throw new EcommIntegrationException("【Lazada】ReadyToShip 响应缺少 packages。");
        }
        for (LazadaFulfillmentModels.ReadyToShipPackageResult item : data.getPackages()) {
            if (!"0".equals(item.getItemErrCode())) {
                throw new EcommIntegrationException("【Lazada】ReadyToShip 失败，packageId=" + item.getPackageId() + "，msg=" + item.getMsg());
            }
        }
    }

    @Override
    public UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId) {
        LazadaOrderModels.OrderItemsResponse orderItemsResponse = lazadaApiClient.getOrderItems(authContext, orderId);
        List<LazadaOrderModels.OrderItem> orderItems = orderItemsResponse.getData();
        if (CollectionUtils.isEmpty(orderItems)) {
            return new UnifiedShipment();
        }

        LinkedHashSet<String> packageIds = orderItems.stream()
            .map(LazadaOrderModels.OrderItem::getPackageId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        LazadaTraceModels.OrderTraceResponse response = lazadaApiClient.getOrderTrace(authContext, orderId, new ArrayList<>(packageIds), "en");
        LazadaTraceModels.OrderTraceResult result = response.getResult();
        if (result == null || Boolean.FALSE.equals(result.getSuccess()) || Boolean.TRUE.equals(result.getNotSuccess())) {
            String message = result != null && result.getErrorCode() != null ? result.getErrorCode().getDisplayMessage() : "未知错误";
            throw new EcommIntegrationException("【Lazada】查询物流轨迹失败，orderId=" + orderId + "，message=" + message);
        }

        UnifiedShipment shipment = new UnifiedShipment();
        if (!CollectionUtils.isEmpty(result.getModule()) && !CollectionUtils.isEmpty(result.getModule().get(0).getPackageDetailInfoList())) {
            LazadaTraceModels.PackageDetail packageDetail = result.getModule().get(0).getPackageDetailInfoList().get(0);
            shipment.setShipmentId(packageDetail.getOfcPackageId());
            shipment.setTrackingNumber(packageDetail.getTrackingNumber());
        }

        if (!CollectionUtils.isEmpty(orderItems)) {
            shipment.setCarrier(orderItems.get(0).getShipmentProvider());
        }

        List<UnifiedTrackingEvent> events = new ArrayList<>();
        if (!CollectionUtils.isEmpty(result.getModule())) {
            for (LazadaTraceModels.TraceModule module : result.getModule()) {
                if (CollectionUtils.isEmpty(module.getPackageDetailInfoList())) {
                    continue;
                }
                for (LazadaTraceModels.PackageDetail packageDetail : module.getPackageDetailInfoList()) {
                    if (!StringUtils.hasText(shipment.getShipmentId())) {
                        shipment.setShipmentId(packageDetail.getOfcPackageId());
                    }
                    if (!StringUtils.hasText(shipment.getTrackingNumber())) {
                        shipment.setTrackingNumber(packageDetail.getTrackingNumber());
                    }
                    if (CollectionUtils.isEmpty(packageDetail.getLogisticDetailInfoList())) {
                        continue;
                    }
                    for (LazadaTraceModels.TraceEvent event : packageDetail.getLogisticDetailInfoList()) {
                        UnifiedTrackingEvent trackingEvent = new UnifiedTrackingEvent();
                        trackingEvent.setDescription(StringUtils.hasText(event.getDescription()) ? event.getDescription() : event.getTitle());
                        trackingEvent.setLocation(event.getPackageLocationName());
                        if (event.getEventTime() != null) {
                            trackingEvent.setTime(Instant.ofEpochMilli(event.getEventTime()));
                        }
                        events.add(trackingEvent);
                    }
                }
            }
        }
        events.sort(Comparator.comparing(UnifiedTrackingEvent::getTime, Comparator.nullsLast(Comparator.naturalOrder())));
        shipment.setTrackingEvents(events);
        if (!events.isEmpty()) {
            UnifiedTrackingEvent latest = events.get(events.size() - 1);
            shipment.setOriginalStatus(latest.getDescription());
            shipment.setUnifiedStatus(latest.getDescription());
        }
        return shipment;
    }

    private AuthContext mapToAuthContext(LazadaAuthModels.TokenResponse response) {
        AuthContext authContext = AuthContext.builder()
            .platform(Platform.LAZADA)
            .accessToken(response.getAccessToken())
            .refreshToken(response.getRefreshToken())
            .accessTokenExpiresAt(response.getExpiresIn() == null ? null : Instant.now().plusSeconds(response.getExpiresIn()))
            .refreshTokenExpiresAt(response.getRefreshExpiresIn() == null ? null : Instant.now().plusSeconds(response.getRefreshExpiresIn()))
            .siteCountry(response.getCountry())
            .accountId(response.getAccountId())
            .accountName(response.getAccount())
            .build();

        List<LazadaAuthModels.CountryUserInfo> countryUserInfoList = response.getCountryUserInfo();
        if (CollectionUtils.isEmpty(countryUserInfoList)) {
            countryUserInfoList = response.getCountryUserInfoList();
        }
        if (!CollectionUtils.isEmpty(countryUserInfoList) && StringUtils.hasText(response.getCountry())) {
            for (LazadaAuthModels.CountryUserInfo countryUserInfo : countryUserInfoList) {
                if (response.getCountry().equalsIgnoreCase(countryUserInfo.getCountry())) {
                    authContext.setSellerId(countryUserInfo.getSellerId());
                    authContext.setShopId(countryUserInfo.getSellerId());
                    break;
                }
            }
        }
        return authContext;
    }

    private Map<String, List<LazadaOrderModels.OrderItem>> loadOrderItemsByOrderIds(AuthContext authContext, List<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }

        Map<String, List<LazadaOrderModels.OrderItem>> result = new HashMap<>();
        for (int index = 0; index < orderIds.size(); index += MAX_BATCH_ORDER_IDS) {
            int end = Math.min(index + MAX_BATCH_ORDER_IDS, orderIds.size());
            List<String> batchOrderIds = orderIds.subList(index, end);
            LazadaOrderModels.MultipleOrderItemsResponse response = lazadaApiClient.getMultipleOrderItems(authContext, batchOrderIds);
            if (CollectionUtils.isEmpty(response.getData())) {
                continue;
            }
            for (LazadaOrderModels.OrderItemsGroup group : response.getData()) {
                result.put(group.getOrderId(), group.getOrderItems());
            }
        }
        return result;
    }

    private UnifiedOrder mapToUnifiedOrder(LazadaOrderModels.Order order, List<LazadaOrderModels.OrderItem> orderItems) {
        UnifiedOrder unifiedOrder = new UnifiedOrder();
        unifiedOrder.setOrderId(order.getOrderId());
        unifiedOrder.setOriginalStatus(resolveOrderStatus(order, orderItems));
        unifiedOrder.setUnifiedStatus(LazadaStatusMapper.toUnifiedStatus(unifiedOrder.getOriginalStatus()));
        if (StringUtils.hasText(order.getCreatedAt())) {
            unifiedOrder.setCreateTime(Instant.parse(order.getCreatedAt()));
        }
        if (StringUtils.hasText(order.getUpdatedAt())) {
            unifiedOrder.setUpdateTime(Instant.parse(order.getUpdatedAt()));
        }
        if (StringUtils.hasText(order.getPrice())) {
            unifiedOrder.setTotalAmount(new BigDecimal(order.getPrice()));
        }
        if (!CollectionUtils.isEmpty(orderItems) && StringUtils.hasText(orderItems.get(0).getCurrency())) {
            unifiedOrder.setCurrency(orderItems.get(0).getCurrency());
        }
        unifiedOrder.setBuyerInfo(buildBuyerInfo(order));

        if (!CollectionUtils.isEmpty(orderItems)) {
            unifiedOrder.setOrderItems(orderItems.stream().map(this::mapToUnifiedOrderItem).collect(Collectors.toList()));
        } else {
            unifiedOrder.setOrderItems(Collections.emptyList());
        }

        UnifiedShipment shipment = new UnifiedShipment();
        shipment.setShippingAddress(resolveShippingAddress(order));
        if (!CollectionUtils.isEmpty(orderItems)) {
            LinkedHashSet<String> packageIds = orderItems.stream()
                .map(LazadaOrderModels.OrderItem::getPackageId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (packageIds.size() == 1) {
                shipment.setShipmentId(packageIds.iterator().next());
            }
            String trackingNumber = resolveSingleTrackingNumber(orderItems);
            if (StringUtils.hasText(trackingNumber)) {
                shipment.setTrackingNumber(trackingNumber);
            }
            String carrier = resolveSingleCarrier(orderItems);
            if (StringUtils.hasText(carrier)) {
                shipment.setCarrier(carrier);
            }
            String shipmentStatus = orderItems.get(0).getStatus();
            shipment.setOriginalStatus(shipmentStatus);
            shipment.setUnifiedStatus(shipmentStatus);
        }
        unifiedOrder.setShipment(shipment);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("original_order", order);
        rawData.put("original_order_items", orderItems == null ? Collections.emptyList() : orderItems);
        unifiedOrder.setRawData(rawData);
        return unifiedOrder;
    }

    private UnifiedOrderItem mapToUnifiedOrderItem(LazadaOrderModels.OrderItem orderItem) {
        UnifiedOrderItem unifiedOrderItem = new UnifiedOrderItem();
        unifiedOrderItem.setOrderLineId(orderItem.getOrderItemId());
        unifiedOrderItem.setProductId(orderItem.getProductId());
        unifiedOrderItem.setProductName(orderItem.getName());
        unifiedOrderItem.setSkuId(orderItem.getSkuId());
        unifiedOrderItem.setSkuName(orderItem.getShopSku());
        unifiedOrderItem.setImageUrl(orderItem.getProductMainImage());
        unifiedOrderItem.setQuantity(1);
        if (StringUtils.hasText(orderItem.getPaidPrice())) {
            unifiedOrderItem.setUnitPrice(new BigDecimal(orderItem.getPaidPrice()));
        } else if (StringUtils.hasText(orderItem.getItemPrice())) {
            unifiedOrderItem.setUnitPrice(new BigDecimal(orderItem.getItemPrice()));
        }
        return unifiedOrderItem;
    }

    private UnifiedAddress resolveShippingAddress(LazadaOrderModels.Order order) {
        if (order == null || order.getAddressShipping() == null) {
            return null;
        }
        LazadaOrderModels.Address address = order.getAddressShipping();
        UnifiedAddress unifiedAddress = new UnifiedAddress();
        unifiedAddress.setFullName(buildReceiverName(address));
        unifiedAddress.setPhone(address.getPhone());
        unifiedAddress.setCountryCode(address.getCountry());
        unifiedAddress.setProvince(address.getAddress3());
        unifiedAddress.setCity(address.getCity());
        if (StringUtils.hasText(address.getAddressDistrict())) {
            unifiedAddress.setDistrict(address.getAddressDistrict());
        } else {
            unifiedAddress.setDistrict(address.getAddressDsitrict());
        }
        unifiedAddress.setStreet(buildStreet(address));
        unifiedAddress.setZipCode(address.getPostCode());
        return unifiedAddress;
    }

    private String resolveOrderStatus(LazadaOrderModels.Order order, List<LazadaOrderModels.OrderItem> orderItems) {
        if (order != null && !CollectionUtils.isEmpty(order.getStatuses())) {
            return order.getStatuses().get(0);
        }
        if (!CollectionUtils.isEmpty(orderItems)) {
            return orderItems.get(0).getStatus();
        }
        return null;
    }

    private String buildBuyerInfo(LazadaOrderModels.Order order) {
        if (order == null) {
            return null;
        }
        String fullName = buildReceiverName(order.getAddressShipping());
        if (StringUtils.hasText(fullName)) {
            return fullName;
        }
        if (StringUtils.hasText(order.getCustomerFirstName()) && StringUtils.hasText(order.getCustomerLastName())) {
            return order.getCustomerFirstName() + " " + order.getCustomerLastName();
        }
        if (StringUtils.hasText(order.getCustomerFirstName())) {
            return order.getCustomerFirstName();
        }
        return order.getCustomerLastName();
    }

    private void validateSellerManagedOrder(String orderId, List<LazadaOrderModels.OrderItem> orderItems) {
        for (LazadaOrderModels.OrderItem orderItem : orderItems) {
            if ("1".equals(orderItem.getDeliveryOptionSof())) {
                throw new EcommIntegrationException("【Lazada】SOF 订单不走 Pack/AWB/RTS 标准链路，orderId=" + orderId);
            }
            if (orderItem.getBizGroup() != null && orderItem.getBizGroup() != 70100L) {
                throw new EcommIntegrationException("【Lazada】当前订单不属于卖家自履约主链路，orderId=" + orderId + "，bizGroup=" + orderItem.getBizGroup());
            }
        }
    }

    private void validateOperationResult(LazadaFulfillmentModels.OperationResult<?> result, String actionName) {
        if (result == null) {
            throw new EcommIntegrationException("【Lazada】" + actionName + " 响应缺少 result。");
        }
        if (Boolean.FALSE.equals(result.getSuccess())) {
            throw new EcommIntegrationException("【Lazada】" + actionName + " 调用失败，errorCode=" + result.getErrorCode() + "，errorMsg=" + result.getErrorMsg());
        }
    }

    private String resolveSingleTrackingNumber(List<LazadaOrderModels.OrderItem> orderItems) {
        LinkedHashSet<String> trackingNumbers = orderItems.stream()
            .map(LazadaOrderModels.OrderItem::getTrackingCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (trackingNumbers.size() == 1) {
            return trackingNumbers.iterator().next();
        }
        return null;
    }

    private String resolveSingleCarrier(List<LazadaOrderModels.OrderItem> orderItems) {
        LinkedHashSet<String> carriers = orderItems.stream()
            .map(LazadaOrderModels.OrderItem::getShipmentProvider)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (carriers.size() == 1) {
            return carriers.iterator().next();
        }
        return null;
    }

    private String buildReceiverName(LazadaOrderModels.Address address) {
        if (address == null) {
            return null;
        }
        if (StringUtils.hasText(address.getFirstName()) && StringUtils.hasText(address.getLastName())) {
            return address.getFirstName() + " " + address.getLastName();
        }
        if (StringUtils.hasText(address.getFirstName())) {
            return address.getFirstName();
        }
        return address.getLastName();
    }

    private String buildReceiverName(UnifiedAddress address) {
        return address == null ? null : address.getFullName();
    }

    private String buildStreet(LazadaOrderModels.Address address) {
        List<String> lines = new ArrayList<>();
        if (StringUtils.hasText(address.getAddress1())) {
            lines.add(address.getAddress1());
        }
        if (StringUtils.hasText(address.getAddress2())) {
            lines.add(address.getAddress2());
        }
        if (StringUtils.hasText(address.getAddress3())) {
            lines.add(address.getAddress3());
        }
        if (StringUtils.hasText(address.getAddress4())) {
            lines.add(address.getAddress4());
        }
        if (StringUtils.hasText(address.getAddress5())) {
            lines.add(address.getAddress5());
        }
        return lines.isEmpty() ? null : String.join(" ", lines);
    }
}
