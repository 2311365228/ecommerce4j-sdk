package com.ecommerce4j.api.platform.shopee;

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
import com.ecommerce4j.api.platform.shopee.dto.ShopeeModels;
import com.ecommerce4j.api.platform.shopee.dto.ShopeeStatusMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Shopee 平台适配器
 */
@Service("SHOPEE")
public class ShopeeAdapter extends AbstractAdapter implements EcommOrderService, EcommFulfillmentService, EcommLogisticsService, EcommAuthorizationService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_ORDER_IDS = 50;
    private static final Duration MAX_ORDER_QUERY_RANGE = Duration.ofDays(15);
    private static final String DEFAULT_ORDER_DETAIL_FIELDS = "buyer_user_id,buyer_username,recipient_address,item_list,package_list,shipping_carrier,payment_method,total_amount";
    private static final String MODE_PICKUP = "pickup";
    private static final String MODE_DROPOFF = "dropoff";
    private static final String MODE_NON_INTEGRATED = "non_integrated";
    private static final String DEFAULT_SHIPPING_DOCUMENT_TYPE = "NORMAL_AIR_WAYBILL";

    @Value("${shopee.partner_id:0}")
    private long partnerId;

    @Value("${shopee.partner_key:}")
    private String partnerKey;

    @Value("${shopee.redirect_uri:}")
    private String redirectUri;

    @Value("${shopee.environment:production}")
    private String environment;

    @Value("${shopee.gateway_region:sg}")
    private String gatewayRegion;

    @Value("${shopee.shipping_document_poll_attempts:5}")
    private int shippingDocumentPollAttempts;

    @Value("${shopee.shipping_document_poll_interval_ms:1000}")
    private long shippingDocumentPollIntervalMs;

    private ShopeeApiClient shopeeApiClient;

    @PostConstruct
    void initClient() {
        this.shopeeApiClient = new ShopeeApiClient(httpClient, downloadClient, objectMapper, partnerId, partnerKey, environment, gatewayRegion);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        if (!StringUtils.hasText(redirectUri)) {
            throw new EcommIntegrationException("【Shopee】回调地址（redirect_uri）不能为空");
        }
        return shopeeApiClient.getAuthorizationUrl(redirectUri, state);
    }

    @Override
    public AuthContext exchangeCodeForTokens(String code) {
        return exchangeCodeForTokens(code, Collections.emptyMap());
    }

    @Override
    public AuthContext exchangeCodeForTokens(String code, Map<String, String> callbackParams) {
        if (!StringUtils.hasText(code)) {
            throw new EcommIntegrationException("【Shopee】授权码（code）不能为空");
        }
        Map<String, String> safeParams = callbackParams == null ? Collections.emptyMap() : callbackParams;
        ShopeeModels.TokenResponse response = shopeeApiClient.getAccessToken(code, safeParams.get("shop_id"), safeParams.get("main_account_id"));
        return mapToAuthContext(response);
    }

    @Override
    public AuthContext refreshTokens(AuthContext authContext) {
        ShopeeModels.TokenResponse response = shopeeApiClient.refreshAccessToken(authContext);
        AuthContext refreshed = mapToAuthContext(response);
        refreshed.setShopId(StringUtils.hasText(refreshed.getShopId()) ? refreshed.getShopId() : authContext.getShopId());
        refreshed.setMerchantId(StringUtils.hasText(refreshed.getMerchantId()) ? refreshed.getMerchantId() : authContext.getMerchantId());
        refreshed.setSiteCountry(authContext.getSiteCountry());
        refreshed.setAccountId(authContext.getAccountId());
        refreshed.setAccountName(authContext.getAccountName());
        return refreshed;
    }

    @Override
    public UnifiedShopInfo getShopInfo(AuthContext authContext) {
        ShopeeModels.ShopInfoResponse response = shopeeApiClient.getShopInfo(authContext);
        String sellerType = Boolean.TRUE.equals(response.getCb()) ? "CB" : "LOCAL";
        if (StringUtils.hasText(response.getShopFulfillmentFlag())) {
            sellerType = sellerType + ":" + response.getShopFulfillmentFlag();
        }
        return UnifiedShopInfo.builder()
            .platform(Platform.SHOPEE)
            .userNickName(response.getShopName())
            .shopId(authContext == null ? null : authContext.getShopId())
            .shopName(response.getShopName())
            .countryId(response.getRegion())
            .cipher("")
            .shopCode(authContext == null ? null : authContext.getShopId())
            .sellerType(sellerType)
            .build();
    }

    @Override
    public PaginatedResult<UnifiedOrder> getOrders(AuthContext authContext, OrderQuery query) {
        if (query == null) {
            throw new EcommIntegrationException("【Shopee】订单查询参数不能为空");
        }
        Instant from = query.getUpdateTimeFrom() != null ? query.getUpdateTimeFrom() : query.getCreateTimeFrom();
        Instant to = query.getUpdateTimeFrom() != null ? query.getUpdateTimeTo() : query.getCreateTimeTo();
        String timeRangeField = query.getUpdateTimeFrom() != null ? "update_time" : "create_time";
        if (from == null) {
            throw new EcommIntegrationException("【Shopee】订单查询需要传入更新时间开始值（updateTimeFrom）或创建时间开始值（createTimeFrom）");
        }
        if (to == null) {
            to = Instant.now();
        }
        if (!from.isBefore(to)) {
            throw new EcommIntegrationException("【Shopee】查询开始时间必须早于结束时间");
        }
        if (Duration.between(from, to).compareTo(MAX_ORDER_QUERY_RANGE) > 0) {
            throw new EcommIntegrationException("【Shopee】订单列表接口单次查询时间范围不能超过 15 天");
        }

        int pageSize = query.getPageSize() > 0 ? Math.min(query.getPageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("time_range_field", timeRangeField);
        queryParameters.put("time_from", String.valueOf(from.getEpochSecond()));
        queryParameters.put("time_to", String.valueOf(to.getEpochSecond()));
        queryParameters.put("page_size", String.valueOf(pageSize));
        queryParameters.put("request_order_status_pending", "true");
        queryParameters.put("response_optional_fields", "order_status");
        if (StringUtils.hasText(query.getPageToken())) {
            queryParameters.put("cursor", query.getPageToken());
        }
        if (StringUtils.hasText(query.getOrderStatus())) {
            queryParameters.put("order_status", query.getOrderStatus());
        }

        ShopeeModels.OrderListResponse response = shopeeApiClient.getOrderList(authContext, queryParameters);
        ShopeeModels.OrderListData data = response.getResponse();
        if (data == null || CollectionUtils.isEmpty(data.getOrderList())) {
            return new PaginatedResult<>(Collections.emptyList(), null);
        }

        List<String> orderIds = data.getOrderList().stream()
            .map(ShopeeModels.OrderSummary::getOrderSn)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        List<UnifiedOrder> details = getOrderDetails(authContext, orderIds);
        String nextPageToken = Boolean.TRUE.equals(data.getMore()) ? data.getNextCursor() : null;
        return new PaginatedResult<>(details, nextPageToken);
    }

    @Override
    public List<UnifiedOrder> getOrderDetails(AuthContext authContext, List<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        List<UnifiedOrder> result = new ArrayList<>();
        for (int index = 0; index < orderIds.size(); index += MAX_BATCH_ORDER_IDS) {
            int end = Math.min(index + MAX_BATCH_ORDER_IDS, orderIds.size());
            List<String> batch = orderIds.subList(index, end);
            ShopeeModels.OrderDetailResponse response = shopeeApiClient.getOrderDetail(authContext, batch, DEFAULT_ORDER_DETAIL_FIELDS, true);
            if (response.getResponse() == null || CollectionUtils.isEmpty(response.getResponse().getOrderList())) {
                continue;
            }
            response.getResponse().getOrderList().stream()
                .map(this::mapToUnifiedOrder)
                .forEach(result::add);
        }
        return result;
    }

    @Override
    public FulfillmentAction prepareFulfillment(AuthContext authContext, String orderId, boolean autoShipIfMissing) {
        ShopeeModels.Order order = loadSingleOrder(authContext, orderId);
        String packageNumber = resolveFirstPackageNumber(order);
        if (!StringUtils.hasText(packageNumber)) {
            throw new EcommIntegrationException("【Shopee】订单缺少包裹号（package_number），订单号=" + orderId);
        }

        if (!autoShipIfMissing) {
            try {
                FulfillmentDocument document = getPackageDocument(authContext, orderId, packageNumber);
                return FulfillmentAction.builder()
                    .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
                    .labelContent(document.getContent())
                    .labelMimeType(document.getMimeType())
                    .trackingNumber(resolveTrackingNumber(authContext, orderId, packageNumber))
                    .receiverName(order.getRecipientAddress() == null ? null : order.getRecipientAddress().getName())
                    .build();
            } catch (EcommIntegrationException e) {
                return FulfillmentAction.builder()
                    .fulfillmentType(FulfillmentType.ERROR)
                    .errorMessage("【Shopee】面单尚未就绪，请先按 getShipmentProviders -> packOrderItems -> getPackageDocument 的顺序完成显式履约原因：" + e.getMessage())
                    .build();
            }
        }

        List<FulfillmentProviderOption> providers = getShipmentProviders(authContext, orderId, Collections.singletonList(packageNumber));
        if (providers.size() != 1) {
            return FulfillmentAction.builder()
                .fulfillmentType(FulfillmentType.ERROR)
                .errorMessage("【Shopee】无法自动选择唯一履约参数，请先调用 getShipmentProviders 获取可用发货方式")
                .build();
        }

        FulfillmentProviderOption selected = providers.get(0);
        FulfillmentPackRequest packRequest = FulfillmentPackRequest.builder()
            .orderId(orderId)
            .orderLineIds(Collections.singletonList(packageNumber))
            .shippingAllocateType(selected.getShippingAllocateType())
            .shipmentProviderCode(selected.getShipmentProviderCode())
            .build();
        List<FulfillmentPackageResult> packResults = packOrderItems(authContext, packRequest);
        FulfillmentDocument document = getPackageDocument(authContext, orderId, packageNumber);
        return FulfillmentAction.builder()
            .fulfillmentType(FulfillmentType.DOWNLOAD_LABEL)
            .labelContent(document.getContent())
            .labelMimeType(document.getMimeType())
            .trackingNumber(packResults.isEmpty() ? null : packResults.get(0).getTrackingNumber())
            .receiverName(order.getRecipientAddress() == null ? null : order.getRecipientAddress().getName())
            .build();
    }

    @Override
    public void submitTracking(AuthContext authContext, String orderId, TrackingInfo trackingInfo) {
        if (trackingInfo == null || !StringUtils.hasText(trackingInfo.getTrackingNumber())) {
            throw new EcommIntegrationException("【Shopee】自有物流发货必须提供运单号（trackingNumber）");
        }
        String packageNumber = firstOrNull(trackingInfo.getOrderLineItemIds());
        if (!StringUtils.hasText(packageNumber)) {
            packageNumber = resolveFirstPackageNumber(loadSingleOrder(authContext, orderId));
        }
        ShopeeModels.ShipOrderRequest request = new ShopeeModels.ShipOrderRequest();
        request.setOrderSn(orderId);
        request.setPackageNumber(packageNumber);
        ShopeeModels.NonIntegratedShipParameter nonIntegrated = new ShopeeModels.NonIntegratedShipParameter();
        nonIntegrated.setTrackingNumber(trackingInfo.getTrackingNumber());
        request.setNonIntegrated(nonIntegrated);
        shopeeApiClient.shipOrder(authContext, request);
    }

    @Override
    public List<FulfillmentProviderOption> getShipmentProviders(AuthContext authContext, String orderId, List<String> orderLineIds) {
        String packageNumber = firstOrNull(orderLineIds);
        if (!StringUtils.hasText(packageNumber)) {
            packageNumber = resolveFirstPackageNumber(loadSingleOrder(authContext, orderId));
        }
        ShopeeModels.ShippingParameterResponse response = shopeeApiClient.getShippingParameter(authContext, orderId, packageNumber);
        ShopeeModels.ShippingParameterData data = response.getResponse();
        if (data == null) {
            return Collections.emptyList();
        }

        List<FulfillmentProviderOption> options = new ArrayList<>();
        if (isNeeded(data.getInfoNeeded(), MODE_PICKUP) && data.getPickup() != null && !CollectionUtils.isEmpty(data.getPickup().getAddressList())) {
            for (ShopeeModels.PickupAddress address : data.getPickup().getAddressList()) {
                if (CollectionUtils.isEmpty(address.getTimeSlotList())) {
                    options.add(providerOption(MODE_PICKUP, encodeProviderCode(MODE_PICKUP, toStringValue(address.getAddressId()), null), buildPickupName(address, null), false));
                    continue;
                }
                for (ShopeeModels.PickupTimeSlot timeSlot : address.getTimeSlotList()) {
                    options.add(providerOption(MODE_PICKUP, encodeProviderCode(MODE_PICKUP, toStringValue(address.getAddressId()), timeSlot.getPickupTimeId()), buildPickupName(address, timeSlot), false));
                }
            }
        }
        if (isNeeded(data.getInfoNeeded(), MODE_DROPOFF) && data.getDropoff() != null) {
            if (!CollectionUtils.isEmpty(data.getDropoff().getBranchList())) {
                for (ShopeeModels.Branch branch : data.getDropoff().getBranchList()) {
                    options.add(providerOption(MODE_DROPOFF, encodeProviderCode(MODE_DROPOFF, toStringValue(branch.getBranchId()), null), buildBranchName(branch), false));
                }
            }
            if (!CollectionUtils.isEmpty(data.getDropoff().getSlugList())) {
                for (ShopeeModels.Slug slug : data.getDropoff().getSlugList()) {
                    options.add(providerOption(MODE_DROPOFF, encodeProviderCode(MODE_DROPOFF, null, slug.getSlug()), slug.getSlugName(), false));
                }
            }
        }
        if (isNeeded(data.getInfoNeeded(), MODE_NON_INTEGRATED)) {
            options.add(providerOption(MODE_NON_INTEGRATED, MODE_NON_INTEGRATED, "Shopee Non-integrated Logistics", options.isEmpty()));
        }
        return options;
    }

    @Override
    public List<FulfillmentPackageResult> packOrderItems(AuthContext authContext, FulfillmentPackRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new EcommIntegrationException("【Shopee】发货请求缺少订单号（orderId）");
        }
        String mode = request.getShippingAllocateType();
        String providerCode = request.getShipmentProviderCode();
        if (!StringUtils.hasText(mode) && StringUtils.hasText(providerCode) && providerCode.contains("|")) {
            mode = providerCode.split("\\|", -1)[0];
        }
        if (!StringUtils.hasText(mode)) {
            throw new EcommIntegrationException("【Shopee】发货请求缺少发货方式（shippingAllocateType），请先调用 getShipmentProviders");
        }

        String packageNumber = firstOrNull(request.getOrderLineIds());
        if (!StringUtils.hasText(packageNumber)) {
            packageNumber = resolveFirstPackageNumber(loadSingleOrder(authContext, request.getOrderId()));
        }

        ShopeeModels.ShipOrderRequest shipOrderRequest = buildShipOrderRequest(request.getOrderId(), packageNumber, mode, providerCode, null);
        shopeeApiClient.shipOrder(authContext, shipOrderRequest);
        String trackingNumber = resolveTrackingNumber(authContext, request.getOrderId(), packageNumber);

        return Collections.singletonList(FulfillmentPackageResult.builder()
            .orderId(request.getOrderId())
            .orderLineIds(Collections.singletonList(packageNumber))
            .packageId(packageNumber)
            .trackingNumber(trackingNumber)
            .shipmentProviderCode(providerCode)
            .shippingAllocateType(mode)
            .retryable(false)
            .message("Shopee 发货接口（ship_order）调用成功")
            .build());
    }

    @Override
    public FulfillmentDocument getPackageDocument(AuthContext authContext, String packageId) {
        throw new EcommIntegrationException("【Shopee】获取面单需要同时传入订单号（orderId）和包裹号（packageId），请调用 getPackageDocument(authContext, orderId, packageId)");
    }

    @Override
    public FulfillmentDocument getPackageDocument(AuthContext authContext, String orderId, String packageId) {
        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(packageId)) {
            throw new EcommIntegrationException("【Shopee】获取面单时订单号（orderId）和包裹号（packageId）都不能为空");
        }
        ShopeeModels.DocumentOrder baseOrder = documentOrder(orderId, packageId, null);
        ShopeeModels.ShippingDocumentParameterResponse parameterResponse =
            shopeeApiClient.getShippingDocumentParameter(authContext, Collections.singletonList(baseOrder));
        String documentType = resolveShippingDocumentType(parameterResponse);
        if (!StringUtils.hasText(documentType)) {
            documentType = DEFAULT_SHIPPING_DOCUMENT_TYPE;
        }

        ShopeeModels.DocumentOrder documentOrder = documentOrder(orderId, packageId, documentType);
        validateDocumentOperation(shopeeApiClient.createShippingDocument(authContext, Collections.singletonList(documentOrder)), "创建面单");
        waitUntilDocumentReady(authContext, documentOrder);
        byte[] content = shopeeApiClient.downloadShippingDocument(authContext, documentType, Collections.singletonList(documentOrder));
        return FulfillmentDocument.builder()
            .packageId(packageId)
            .mimeType("application/pdf")
            .content(content)
            .build();
    }

    @Override
    public UnifiedShipment getTrackingEvents(AuthContext authContext, String orderId) {
        ShopeeModels.Order order = loadSingleOrder(authContext, orderId);
        String packageNumber = resolveFirstPackageNumber(order);
        ShopeeModels.TrackingInfoResponse response = shopeeApiClient.getTrackingInfo(authContext, orderId, packageNumber);
        ShopeeModels.TrackingInfoData data = response.getResponse();
        UnifiedShipment shipment = new UnifiedShipment();
        shipment.setShipmentId(packageNumber);
        shipment.setCarrier(resolveCarrier(order));
        shipment.setShippingAddress(mapToUnifiedAddress(order.getRecipientAddress()));
        shipment.setTrackingNumber(resolveTrackingNumber(authContext, orderId, packageNumber));
        if (data == null) {
            shipment.setTrackingEvents(Collections.emptyList());
            return shipment;
        }
        shipment.setOriginalStatus(data.getLogisticsStatus());
        shipment.setUnifiedStatus(data.getLogisticsStatus());

        List<UnifiedTrackingEvent> events = new ArrayList<>();
        if (!CollectionUtils.isEmpty(data.getTrackingInfo())) {
            for (ShopeeModels.TrackingEvent event : data.getTrackingInfo()) {
                UnifiedTrackingEvent unifiedEvent = new UnifiedTrackingEvent();
                unifiedEvent.setDescription(event.getDescription());
                if (event.getUpdateTime() != null) {
                    unifiedEvent.setTime(Instant.ofEpochSecond(event.getUpdateTime()));
                }
                unifiedEvent.setLocation(event.getLogisticsStatus());
                events.add(unifiedEvent);
            }
        }
        events.sort(Comparator.comparing(UnifiedTrackingEvent::getTime, Comparator.nullsLast(Comparator.naturalOrder())));
        shipment.setTrackingEvents(events);
        return shipment;
    }

    private AuthContext mapToAuthContext(ShopeeModels.TokenResponse response) {
        if (response == null) {
            throw new EcommIntegrationException("【Shopee】授权响应为空");
        }
        String shopId = response.getShopId() != null ? response.getShopId().toString() : firstLongAsString(response.getShopIdList());
        String merchantId = response.getMerchantId() != null ? response.getMerchantId().toString() : firstLongAsString(response.getMerchantIdList());
        return AuthContext.builder()
            .platform(Platform.SHOPEE)
            .accessToken(response.getAccessToken())
            .refreshToken(response.getRefreshToken())
            .accessTokenExpiresAt(resolveAccessTokenExpiresAt(response.getExpireIn()))
            .refreshTokenExpiresAt(Instant.now().plus(Duration.ofDays(30)))
            .shopId(shopId)
            .sellerId(shopId)
            .merchantId(merchantId)
            .shopIds(longListToStringList(response.getShopIdList()))
            .merchantIds(longListToStringList(response.getMerchantIdList()))
            .build();
    }

    private Instant resolveAccessTokenExpiresAt(Long expireIn) {
        if (expireIn == null) {
            return null;
        }
        long now = Instant.now().getEpochSecond();
        if (expireIn > now) {
            return Instant.ofEpochSecond(expireIn);
        }
        return Instant.now().plusSeconds(expireIn);
    }

    private ShopeeModels.Order loadSingleOrder(AuthContext authContext, String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new EcommIntegrationException("【Shopee】订单号（orderId）不能为空");
        }
        ShopeeModels.OrderDetailResponse response = shopeeApiClient.getOrderDetail(authContext, Collections.singletonList(orderId), DEFAULT_ORDER_DETAIL_FIELDS, true);
        if (response.getResponse() == null || CollectionUtils.isEmpty(response.getResponse().getOrderList())) {
            throw new EcommIntegrationException("【Shopee】未找到订单，订单号=" + orderId);
        }
        return response.getResponse().getOrderList().get(0);
    }

    private UnifiedOrder mapToUnifiedOrder(ShopeeModels.Order order) {
        UnifiedOrder unifiedOrder = new UnifiedOrder();
        unifiedOrder.setOrderId(order.getOrderSn());
        unifiedOrder.setOriginalStatus(order.getOrderStatus());
        unifiedOrder.setUnifiedStatus(ShopeeStatusMapper.toUnifiedStatus(order.getOrderStatus()));
        unifiedOrder.setCurrency(order.getCurrency());
        unifiedOrder.setTotalAmount(order.getTotalAmount());
        unifiedOrder.setBuyerInfo(StringUtils.hasText(order.getBuyerUsername()) ? order.getBuyerUsername() : String.valueOf(order.getBuyerUserId()));
        if (order.getCreateTime() != null) {
            unifiedOrder.setCreateTime(Instant.ofEpochSecond(order.getCreateTime()));
        }
        if (order.getUpdateTime() != null) {
            unifiedOrder.setUpdateTime(Instant.ofEpochSecond(order.getUpdateTime()));
        }
        unifiedOrder.setOrderItems(CollectionUtils.isEmpty(order.getItemList())
            ? Collections.emptyList()
            : order.getItemList().stream().map(this::mapToUnifiedOrderItem).collect(Collectors.toList()));

        UnifiedShipment shipment = new UnifiedShipment();
        String packageNumber = resolveFirstPackageNumber(order);
        shipment.setShipmentId(packageNumber);
        shipment.setCarrier(resolveCarrier(order));
        shipment.setShippingAddress(mapToUnifiedAddress(order.getRecipientAddress()));
        ShopeeModels.PackageInfo packageInfo = firstPackage(order);
        if (packageInfo != null) {
            shipment.setOriginalStatus(packageInfo.getLogisticsStatus());
            shipment.setUnifiedStatus(packageInfo.getLogisticsStatus());
        }
        unifiedOrder.setShipment(shipment);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("original_order", order);
        unifiedOrder.setRawData(rawData);
        return unifiedOrder;
    }

    private UnifiedOrderItem mapToUnifiedOrderItem(ShopeeModels.OrderItem item) {
        UnifiedOrderItem unifiedItem = new UnifiedOrderItem();
        unifiedItem.setOrderLineId(item.getOrderItemId() == null ? toStringValue(item.getItemId()) : item.getOrderItemId().toString());
        unifiedItem.setProductId(toStringValue(item.getItemId()));
        unifiedItem.setProductName(item.getItemName());
        unifiedItem.setSkuId(toStringValue(item.getModelId()));
        unifiedItem.setSkuName(StringUtils.hasText(item.getModelSku()) ? item.getModelSku() : item.getModelName());
        unifiedItem.setImageUrl(item.getImageInfo() == null ? null : item.getImageInfo().getImageUrl());
        unifiedItem.setQuantity(item.getModelQuantityPurchased());
        BigDecimal unitPrice = item.getModelDiscountedPrice() != null ? item.getModelDiscountedPrice() : item.getModelOriginalPrice();
        unifiedItem.setUnitPrice(unitPrice);
        return unifiedItem;
    }

    private UnifiedAddress mapToUnifiedAddress(ShopeeModels.RecipientAddress address) {
        if (address == null) {
            return null;
        }
        UnifiedAddress unifiedAddress = new UnifiedAddress();
        unifiedAddress.setFullName(address.getName());
        unifiedAddress.setPhone(address.getPhone());
        unifiedAddress.setCountryCode(address.getRegion());
        unifiedAddress.setProvince(address.getState());
        unifiedAddress.setCity(address.getCity());
        unifiedAddress.setDistrict(StringUtils.hasText(address.getDistrict()) ? address.getDistrict() : address.getTown());
        unifiedAddress.setStreet(address.getFullAddress());
        unifiedAddress.setZipCode(address.getZipcode());
        return unifiedAddress;
    }

    private ShopeeModels.ShipOrderRequest buildShipOrderRequest(String orderId,
                                                                String packageNumber,
                                                                String mode,
                                                                String providerCode,
                                                                String trackingNumber) {
        ShopeeModels.ShipOrderRequest shipOrderRequest = new ShopeeModels.ShipOrderRequest();
        shipOrderRequest.setOrderSn(orderId);
        shipOrderRequest.setPackageNumber(packageNumber);
        ProviderCodeParts parts = ProviderCodeParts.parse(providerCode);
        if (MODE_PICKUP.equalsIgnoreCase(mode)) {
            ShopeeModels.PickupShipParameter pickup = new ShopeeModels.PickupShipParameter();
            pickup.setAddressId(parseLong(parts.primary, "address_id"));
            pickup.setPickupTimeId(parts.secondary);
            pickup.setTrackingNumber(trackingNumber);
            shipOrderRequest.setPickup(pickup);
        } else if (MODE_DROPOFF.equalsIgnoreCase(mode)) {
            ShopeeModels.DropoffShipParameter dropoff = new ShopeeModels.DropoffShipParameter();
            dropoff.setBranchId(parseLong(parts.primary, "branch_id"));
            dropoff.setSlug(parts.secondary);
            dropoff.setTrackingNumber(trackingNumber);
            shipOrderRequest.setDropoff(dropoff);
        } else if (MODE_NON_INTEGRATED.equalsIgnoreCase(mode)) {
            if (!StringUtils.hasText(trackingNumber)) {
                throw new EcommIntegrationException("【Shopee】自有物流（non_integrated）发货必须通过 submitTracking 提供运单号（trackingNumber）");
            }
            ShopeeModels.NonIntegratedShipParameter nonIntegrated = new ShopeeModels.NonIntegratedShipParameter();
            nonIntegrated.setTrackingNumber(trackingNumber);
            shipOrderRequest.setNonIntegrated(nonIntegrated);
        } else {
            throw new EcommIntegrationException("【Shopee】不支持的履约模式：" + mode);
        }
        return shipOrderRequest;
    }

    private String resolveTrackingNumber(AuthContext authContext, String orderId, String packageNumber) {
        ShopeeModels.TrackingNumberResponse response = shopeeApiClient.getTrackingNumber(authContext, orderId, packageNumber);
        if (response.getResponse() == null) {
            return null;
        }
        if (StringUtils.hasText(response.getResponse().getTrackingNumber())) {
            return response.getResponse().getTrackingNumber();
        }
        return response.getResponse().getFirstMileTrackingNumber();
    }

    private void waitUntilDocumentReady(AuthContext authContext, ShopeeModels.DocumentOrder documentOrder) {
        int attempts = Math.max(1, shippingDocumentPollAttempts);
        for (int index = 0; index < attempts; index++) {
            ShopeeModels.ShippingDocumentOperationResponse response =
                shopeeApiClient.getShippingDocumentResult(authContext, Collections.singletonList(documentOrder));
            ShopeeModels.ShippingDocumentOperationResult result = firstDocumentResult(response);
            if (result != null && StringUtils.hasText(result.getFailError())) {
                throw new EcommIntegrationException("【Shopee】面单生成失败，错误码=" + result.getFailError() + "，错误信息=" + result.getFailMessage());
            }
            if (result != null && "READY".equalsIgnoreCase(result.getStatus())) {
                return;
            }
            if (index + 1 < attempts) {
                sleepBeforeNextPoll();
            }
        }
        throw new EcommIntegrationException("【Shopee】面单生成超时，请稍后重试");
    }

    private void sleepBeforeNextPoll() {
        if (shippingDocumentPollIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(shippingDocumentPollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EcommIntegrationException("【Shopee】等待面单生成时线程被中断", e);
        }
    }

    private void validateDocumentOperation(ShopeeModels.ShippingDocumentOperationResponse response, String actionName) {
        ShopeeModels.ShippingDocumentOperationResult result = firstDocumentResult(response);
        if (result != null && StringUtils.hasText(result.getFailError())) {
            throw new EcommIntegrationException("【Shopee】" + actionName + "失败，错误码=" + result.getFailError() + "，错误信息=" + result.getFailMessage());
        }
    }

    private ShopeeModels.ShippingDocumentOperationResult firstDocumentResult(ShopeeModels.ShippingDocumentOperationResponse response) {
        if (response == null || response.getResponse() == null || CollectionUtils.isEmpty(response.getResponse().getResultList())) {
            return null;
        }
        return response.getResponse().getResultList().get(0);
    }

    private String resolveShippingDocumentType(ShopeeModels.ShippingDocumentParameterResponse response) {
        if (response == null || response.getResponse() == null || CollectionUtils.isEmpty(response.getResponse().getResultList())) {
            return null;
        }
        ShopeeModels.ShippingDocumentParameterResult result = response.getResponse().getResultList().get(0);
        if (StringUtils.hasText(result.getFailError())) {
            throw new EcommIntegrationException("【Shopee】查询面单参数失败，错误码=" + result.getFailError() + "，错误信息=" + result.getFailMessage());
        }
        if (StringUtils.hasText(result.getSuggestShippingDocumentType())) {
            return result.getSuggestShippingDocumentType();
        }
        if (!CollectionUtils.isEmpty(result.getSelectableShippingDocumentType())) {
            return result.getSelectableShippingDocumentType().get(0);
        }
        return null;
    }

    private ShopeeModels.DocumentOrder documentOrder(String orderId, String packageNumber, String documentType) {
        ShopeeModels.DocumentOrder order = new ShopeeModels.DocumentOrder();
        order.setOrderSn(orderId);
        order.setPackageNumber(packageNumber);
        order.setShippingDocumentType(documentType);
        return order;
    }

    private boolean isNeeded(ShopeeModels.InfoNeeded infoNeeded, String mode) {
        if (infoNeeded == null) {
            return true;
        }
        if (MODE_PICKUP.equals(mode)) {
            return Boolean.TRUE.equals(infoNeeded.getPickup());
        }
        if (MODE_DROPOFF.equals(mode)) {
            return Boolean.TRUE.equals(infoNeeded.getDropoff());
        }
        if (MODE_NON_INTEGRATED.equals(mode)) {
            return Boolean.TRUE.equals(infoNeeded.getNonIntegrated());
        }
        return false;
    }

    private FulfillmentProviderOption providerOption(String mode, String code, String name, boolean platformDefault) {
        return FulfillmentProviderOption.builder()
            .shippingAllocateType(mode)
            .shipmentProviderCode(code)
            .shipmentProviderName(name)
            .platformDefault(platformDefault)
            .build();
    }

    private String buildPickupName(ShopeeModels.PickupAddress address, ShopeeModels.PickupTimeSlot timeSlot) {
        String base = "Pickup " + nullToEmpty(address.getAddress());
        if (timeSlot != null && StringUtils.hasText(timeSlot.getTimeText())) {
            return base + " " + timeSlot.getTimeText();
        }
        return base;
    }

    private String buildBranchName(ShopeeModels.Branch branch) {
        if (StringUtils.hasText(branch.getAddress())) {
            return "Dropoff " + branch.getAddress();
        }
        return "Dropoff " + branch.getBranchId();
    }

    private String encodeProviderCode(String mode, String primary, String secondary) {
        return mode + "|" + nullToEmpty(primary) + "|" + nullToEmpty(secondary);
    }

    private String resolveFirstPackageNumber(ShopeeModels.Order order) {
        ShopeeModels.PackageInfo packageInfo = firstPackage(order);
        return packageInfo == null ? null : packageInfo.getPackageNumber();
    }

    private String resolveCarrier(ShopeeModels.Order order) {
        ShopeeModels.PackageInfo packageInfo = firstPackage(order);
        if (packageInfo != null && StringUtils.hasText(packageInfo.getShippingCarrier())) {
            return packageInfo.getShippingCarrier();
        }
        return order.getShippingCarrier();
    }

    private ShopeeModels.PackageInfo firstPackage(ShopeeModels.Order order) {
        if (order == null || CollectionUtils.isEmpty(order.getPackageList())) {
            return null;
        }
        return order.getPackageList().get(0);
    }

    private String firstOrNull(List<String> values) {
        return CollectionUtils.isEmpty(values) ? null : values.get(0);
    }

    private Long parseLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new EcommIntegrationException("【Shopee】" + fieldName + " 必须是数字类型", e);
        }
    }

    private String firstLongAsString(List<Long> values) {
        return CollectionUtils.isEmpty(values) ? null : toStringValue(values.get(0));
    }

    private List<String> longListToStringList(List<Long> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class ProviderCodeParts {

        private final String primary;

        private final String secondary;

        private ProviderCodeParts(String primary, String secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        private static ProviderCodeParts parse(String providerCode) {
            if (!StringUtils.hasText(providerCode) || !providerCode.contains("|")) {
                return new ProviderCodeParts(null, null);
            }
            String[] parts = providerCode.split("\\|", -1);
            String primary = parts.length > 1 && StringUtils.hasText(parts[1]) ? parts[1] : null;
            String secondary = parts.length > 2 && StringUtils.hasText(parts[2]) ? parts[2] : null;
            return new ProviderCodeParts(primary, secondary);
        }
    }
}
