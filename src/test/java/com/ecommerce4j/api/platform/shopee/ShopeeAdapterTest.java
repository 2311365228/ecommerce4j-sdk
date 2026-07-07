package com.ecommerce4j.api.platform.shopee;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.FulfillmentDocument;
import com.ecommerce4j.api.dto.FulfillmentPackRequest;
import com.ecommerce4j.api.dto.FulfillmentPackageResult;
import com.ecommerce4j.api.dto.FulfillmentProviderOption;
import com.ecommerce4j.api.dto.OrderQuery;
import com.ecommerce4j.api.dto.PaginatedResult;
import com.ecommerce4j.api.dto.TrackingInfo;
import com.ecommerce4j.api.dto.UnifiedOrder;
import com.ecommerce4j.api.dto.UnifiedShipment;
import com.ecommerce4j.api.enums.Platform;
import com.ecommerce4j.api.enums.UnifiedOrderStatus;
import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopeeAdapterTest {

    private static final long PARTNER_ID = 1001L;
    private static final String PARTNER_KEY = "partner-key";

    private MockWebServer server;
    private ObjectMapper objectMapper;
    private ShopeeApiClient client;
    private AuthContext authContext;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OkHttpClient okHttpClient = new OkHttpClient();
        client = ShopeeApiClient.forBaseUrls(
            okHttpClient,
            okHttpClient,
            objectMapper,
            PARTNER_ID,
            PARTNER_KEY,
            server.url("").toString(),
            server.url("/auth").toString());
        authContext = AuthContext.builder()
            .platform(Platform.SHOPEE)
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .shopId("12345")
            .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    @DisplayName("Shopee Public/Shop 签名串符合官方 v2 规则")
    void signsPublicAndShopRequests() {
        long timestamp = 1700000000L;

        assertEquals(
            hmac(PARTNER_KEY, PARTNER_ID + ShopeeApiClient.PATH_AUTH_TOKEN_GET + timestamp),
            client.signPublic(ShopeeApiClient.PATH_AUTH_TOKEN_GET, timestamp));
        assertEquals(
            hmac(PARTNER_KEY, PARTNER_ID + ShopeeApiClient.PATH_ORDER_GET_LIST + timestamp + "access-token" + "12345"),
            client.signShop(ShopeeApiClient.PATH_ORDER_GET_LIST, timestamp, "access-token", "12345"));
    }

    @Test
    @DisplayName("Shopee 授权 URL 和多网关域名按环境解析")
    void buildsAuthorizationUrlAndResolvesGatewayDomains() {
        String authorizationUrl = client.getAuthorizationUrl("https://example.com/callback", "state-1");

        assertTrue(authorizationUrl.startsWith(server.url("/auth").toString()));
        assertTrue(authorizationUrl.contains("partner_id=1001"));
        assertTrue(authorizationUrl.contains("auth_type=seller"));
        assertTrue(authorizationUrl.contains("response_type=code"));
        assertTrue(authorizationUrl.contains("state=state-1"));

        ShopeeApiClient productionSg = new ShopeeApiClient(new OkHttpClient(), new OkHttpClient(), objectMapper, PARTNER_ID, PARTNER_KEY, "production", "sg");
        ShopeeApiClient productionCn = new ShopeeApiClient(new OkHttpClient(), new OkHttpClient(), objectMapper, PARTNER_ID, PARTNER_KEY, "production", "cn");
        ShopeeApiClient productionBr = new ShopeeApiClient(new OkHttpClient(), new OkHttpClient(), objectMapper, PARTNER_ID, PARTNER_KEY, "production", "br");
        ShopeeApiClient sandboxSg = new ShopeeApiClient(new OkHttpClient(), new OkHttpClient(), objectMapper, PARTNER_ID, PARTNER_KEY, "sandbox", "sg");

        assertEquals("https://partner.shopeemobile.com", productionSg.getGatewayBaseUrl());
        assertEquals("https://openplatform.shopee.cn", productionCn.getGatewayBaseUrl());
        assertEquals("https://openplatform.shopee.com.br", productionBr.getGatewayBaseUrl());
        assertEquals("https://openplatform.sandbox.test-stable.shopee.sg", sandboxSg.getGatewayBaseUrl());
        assertEquals("https://open.shopee.com/auth", productionSg.getAuthBaseUrl());
        assertEquals("https://open.sandbox.test-stable.shopee.com/auth", sandboxSg.getAuthBaseUrl());
    }

    @Test
    @DisplayName("Shopee token 换取和刷新会带上回调店铺参数")
    void exchangesAndRefreshesTokens() throws Exception {
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-token",
              "partner_id": 1001,
              "shop_id": 12345,
              "shop_id_list": [12345, 67890],
              "merchant_id_list": [222],
              "access_token": "access-new",
              "refresh_token": "refresh-new",
              "expire_in": 14400
            }
            """);
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-refresh",
              "shop_id": 12345,
              "access_token": "access-refreshed",
              "refresh_token": "refresh-refreshed",
              "expire_in": 14400
            }
            """);

        ShopeeAdapter adapter = newAdapter();
        AuthContext exchanged = adapter.exchangeCodeForTokens("code-1", Collections.singletonMap("shop_id", "12345"));
        AuthContext refreshed = adapter.refreshTokens(authContext);

        assertEquals("access-new", exchanged.getAccessToken());
        assertEquals("refresh-new", exchanged.getRefreshToken());
        assertEquals("12345", exchanged.getShopId());
        assertEquals(List.of("12345", "67890"), exchanged.getShopIds());
        assertEquals(List.of("222"), exchanged.getMerchantIds());
        assertEquals("access-refreshed", refreshed.getAccessToken());
        assertEquals("12345", refreshed.getShopId());

        RecordedRequest tokenRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_AUTH_TOKEN_GET, tokenRequest.getRequestUrl().encodedPath());
        assertPublicSignature(tokenRequest, ShopeeApiClient.PATH_AUTH_TOKEN_GET);
        JsonNode tokenBody = readBody(tokenRequest);
        assertEquals("code-1", tokenBody.path("code").asText());
        assertEquals(PARTNER_ID, tokenBody.path("partner_id").asLong());
        assertEquals(12345L, tokenBody.path("shop_id").asLong());

        RecordedRequest refreshRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_AUTH_ACCESS_TOKEN_GET, refreshRequest.getRequestUrl().encodedPath());
        assertPublicSignature(refreshRequest, ShopeeApiClient.PATH_AUTH_ACCESS_TOKEN_GET);
        JsonNode refreshBody = readBody(refreshRequest);
        assertEquals("refresh-token", refreshBody.path("refresh_token").asText());
        assertEquals(12345L, refreshBody.path("shop_id").asLong());
    }

    @Test
    @DisplayName("Shopee 错误响应使用中文异常格式")
    void throwsChineseExceptionForShopeeErrorResponse() {
        enqueueJson("""
            {
              "error": "error_auth",
              "message": "签名错误",
              "request_id": "req-error"
            }
            """);

        EcommIntegrationException exception = assertThrows(EcommIntegrationException.class, () -> client.getShopInfo(authContext));

        assertTrue(exception.getMessage().contains("【Shopee】接口调用失败"));
        assertTrue(exception.getMessage().contains("错误码=error_auth"));
        assertTrue(exception.getMessage().contains("错误信息=签名错误"));
        assertTrue(exception.getMessage().contains("请求ID=req-error"));
    }

    @Test
    @DisplayName("Shopee 订单列表分页和详情映射正确")
    void mapsOrdersWithCursorAndDetails() throws Exception {
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-list",
              "response": {
                "more": true,
                "next_cursor": "cursor-2",
                "order_list": [
                  {"order_sn": "SN1", "order_status": "READY_TO_SHIP"}
                ]
              }
            }
            """);
        enqueueJson(orderDetailJson("READY_TO_SHIP"));

        ShopeeAdapter adapter = newAdapter();
        OrderQuery query = OrderQuery.builder()
            .updateTimeFrom(Instant.ofEpochSecond(1700000000L))
            .updateTimeTo(Instant.ofEpochSecond(1700003600L))
            .pageSize(200)
            .pageToken("cursor-1")
            .orderStatus("READY_TO_SHIP")
            .build();
        PaginatedResult<UnifiedOrder> result = adapter.getOrders(authContext, query);

        assertEquals("cursor-2", result.getNextPageToken());
        assertEquals(1, result.getData().size());
        UnifiedOrder order = result.getData().get(0);
        assertEquals("SN1", order.getOrderId());
        assertEquals(UnifiedOrderStatus.READY_FOR_FULFILLMENT, order.getUnifiedStatus());
        assertEquals("READY_TO_SHIP", order.getOriginalStatus());
        assertEquals("SGD", order.getCurrency());
        assertEquals(new BigDecimal("12.34"), order.getTotalAmount());
        assertEquals("buyer-1", order.getBuyerInfo());
        assertEquals("PKG1", order.getShipment().getShipmentId());
        assertEquals("SPX Express", order.getShipment().getCarrier());
        assertEquals("Alice", order.getShipment().getShippingAddress().getFullName());
        assertEquals(1, order.getOrderItems().size());
        assertEquals("Keyboard", order.getOrderItems().get(0).getProductName());

        RecordedRequest listRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_ORDER_GET_LIST, listRequest.getRequestUrl().encodedPath());
        assertShopSignature(listRequest, ShopeeApiClient.PATH_ORDER_GET_LIST);
        assertEquals("update_time", listRequest.getRequestUrl().queryParameter("time_range_field"));
        assertEquals("100", listRequest.getRequestUrl().queryParameter("page_size"));
        assertEquals("cursor-1", listRequest.getRequestUrl().queryParameter("cursor"));
        assertEquals("READY_TO_SHIP", listRequest.getRequestUrl().queryParameter("order_status"));

        RecordedRequest detailRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_ORDER_GET_DETAIL, detailRequest.getRequestUrl().encodedPath());
        assertEquals("SN1", detailRequest.getRequestUrl().queryParameter("order_sn_list"));
        assertTrue(detailRequest.getRequestUrl().queryParameter("response_optional_fields").contains("recipient_address"));
    }

    @Test
    @DisplayName("Shopee 订单查询超过 15 天时直接返回中文异常")
    void rejectsOrderQueryRangeOverFifteenDays() throws Exception {
        ShopeeAdapter adapter = newAdapter();
        OrderQuery query = OrderQuery.builder()
            .createTimeFrom(Instant.ofEpochSecond(1700000000L))
            .createTimeTo(Instant.ofEpochSecond(1700000000L).plus(Duration.ofDays(16)))
            .build();

        EcommIntegrationException exception = assertThrows(EcommIntegrationException.class, () -> adapter.getOrders(authContext, query));

        assertEquals("【Shopee】订单列表接口单次查询时间范围不能超过 15 天。", exception.getMessage());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Shopee 订单详情按 50 单批量查询")
    void batchesOrderDetailsByFiftyOrders() throws Exception {
        enqueueJson(emptyOrderDetailJson());
        enqueueJson(emptyOrderDetailJson());
        List<String> orderIds = new ArrayList<>();
        for (int index = 1; index <= 51; index++) {
            orderIds.add("SN" + index);
        }

        newAdapter().getOrderDetails(authContext, orderIds);

        RecordedRequest first = takeRequest();
        RecordedRequest second = takeRequest();
        assertEquals(50, first.getRequestUrl().queryParameter("order_sn_list").split(",").length);
        assertEquals("SN51", second.getRequestUrl().queryParameter("order_sn_list"));
    }

    @Test
    @DisplayName("Shopee 履约参数映射 pickup/dropoff/non_integrated")
    void mapsShippingParameterOptions() throws Exception {
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-shipping-parameter",
              "response": {
                "info_needed": {
                  "pickup": true,
                  "dropoff": true,
                  "non_integrated": true
                },
                "pickup": {
                  "address_list": [
                    {
                      "address_id": 10,
                      "address": "Warehouse A",
                      "time_slot_list": [
                        {"pickup_time_id": "TS1", "time_text": "09:00-12:00"}
                      ]
                    }
                  ]
                },
                "dropoff": {
                  "branch_list": [
                    {"branch_id": 20, "address": "Branch A"}
                  ],
                  "slug_list": [
                    {"slug": "slug-a", "slug_name": "Slug A"}
                  ]
                }
              }
            }
            """);

        List<FulfillmentProviderOption> options = newAdapter().getShipmentProviders(authContext, "SN1", List.of("PKG1"));

        assertEquals(4, options.size());
        assertEquals("pickup", options.get(0).getShippingAllocateType());
        assertEquals("pickup|10|TS1", options.get(0).getShipmentProviderCode());
        assertTrue(options.get(0).getShipmentProviderName().contains("Warehouse A"));
        assertEquals("dropoff|20|", options.get(1).getShipmentProviderCode());
        assertEquals("dropoff||slug-a", options.get(2).getShipmentProviderCode());
        assertEquals("non_integrated", options.get(3).getShippingAllocateType());
        assertFalse(options.get(3).isPlatformDefault());

        RecordedRequest request = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_GET_SHIPPING_PARAMETER, request.getRequestUrl().encodedPath());
        assertEquals("SN1", request.getRequestUrl().queryParameter("order_sn"));
        assertEquals("PKG1", request.getRequestUrl().queryParameter("package_number"));
    }

    @Test
    @DisplayName("Shopee ship_order 请求体只发送选中的履约参数")
    void shipsOrderWithSelectedPickupParameter() throws Exception {
        enqueueJson("""
            {"error": "", "message": "", "request_id": "req-ship"}
            """);
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-tracking-number",
              "response": {"tracking_number": "TRK1"}
            }
            """);

        FulfillmentPackRequest request = FulfillmentPackRequest.builder()
            .orderId("SN1")
            .orderLineIds(List.of("PKG1"))
            .shippingAllocateType("pickup")
            .shipmentProviderCode("pickup|10|TS1")
            .build();
        List<FulfillmentPackageResult> result = newAdapter().packOrderItems(authContext, request);

        assertEquals(1, result.size());
        assertEquals("PKG1", result.get(0).getPackageId());
        assertEquals("TRK1", result.get(0).getTrackingNumber());
        assertEquals("Shopee 发货接口（ship_order）调用成功", result.get(0).getMessage());

        RecordedRequest shipRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_SHIP_ORDER, shipRequest.getRequestUrl().encodedPath());
        JsonNode body = readBody(shipRequest);
        assertEquals("SN1", body.path("order_sn").asText());
        assertEquals("PKG1", body.path("package_number").asText());
        assertEquals(10L, body.path("pickup").path("address_id").asLong());
        assertEquals("TS1", body.path("pickup").path("pickup_time_id").asText());
        assertFalse(body.has("dropoff"));
        assertFalse(body.has("non_integrated"));
        assertFalse(body.path("pickup").has("tracking_number"));

        RecordedRequest trackingRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_GET_TRACKING_NUMBER, trackingRequest.getRequestUrl().encodedPath());
    }

    @Test
    @DisplayName("Shopee 自有物流回填运单号使用 non_integrated 发货参数")
    void submitsTrackingNumberForNonIntegratedLogistics() throws Exception {
        enqueueJson("""
            {"error": "", "message": "", "request_id": "req-submit-tracking"}
            """);

        TrackingInfo trackingInfo = TrackingInfo.builder()
            .trackingNumber("SELF-TRK-1")
            .orderLineItemIds(List.of("PKG1"))
            .build();
        newAdapter().submitTracking(authContext, "SN1", trackingInfo);

        RecordedRequest request = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_SHIP_ORDER, request.getRequestUrl().encodedPath());
        JsonNode body = readBody(request);
        assertEquals("SN1", body.path("order_sn").asText());
        assertEquals("PKG1", body.path("package_number").asText());
        assertEquals("SELF-TRK-1", body.path("non_integrated").path("tracking_number").asText());
        assertFalse(body.has("pickup"));
        assertFalse(body.has("dropoff"));
    }

    @Test
    @DisplayName("Shopee 面单按参数、创建、轮询、下载顺序执行")
    void createsPollsAndDownloadsShippingDocument() throws Exception {
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-doc-parameter",
              "response": {
                "result_list": [
                  {
                    "order_sn": "SN1",
                    "package_number": "PKG1",
                    "suggest_shipping_document_type": "NORMAL_AIR_WAYBILL"
                  }
                ]
              }
            }
            """);
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-doc-create",
              "response": {
                "result_list": [
                  {"order_sn": "SN1", "package_number": "PKG1", "status": "PROCESSING"}
                ]
              }
            }
            """);
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-doc-result",
              "response": {
                "result_list": [
                  {"order_sn": "SN1", "package_number": "PKG1", "status": "READY"}
                ]
              }
            }
            """);
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/pdf")
            .setBody(new Buffer().write(pdf)));

        FulfillmentDocument document = newAdapter().getPackageDocument(authContext, "SN1", "PKG1");

        assertEquals("PKG1", document.getPackageId());
        assertEquals("application/pdf", document.getMimeType());
        assertArrayEquals(pdf, document.getContent());

        RecordedRequest parameterRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_PARAMETER, parameterRequest.getRequestUrl().encodedPath());
        assertFalse(readBody(parameterRequest).path("order_list").get(0).has("shipping_document_type"));

        RecordedRequest createRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_CREATE_SHIPPING_DOCUMENT, createRequest.getRequestUrl().encodedPath());
        assertEquals("NORMAL_AIR_WAYBILL", readBody(createRequest).path("order_list").get(0).path("shipping_document_type").asText());

        RecordedRequest resultRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_GET_SHIPPING_DOCUMENT_RESULT, resultRequest.getRequestUrl().encodedPath());

        RecordedRequest downloadRequest = takeRequest();
        assertEquals(ShopeeApiClient.PATH_LOGISTICS_DOWNLOAD_SHIPPING_DOCUMENT, downloadRequest.getRequestUrl().encodedPath());
        assertEquals("NORMAL_AIR_WAYBILL", readBody(downloadRequest).path("shipping_document_type").asText());
    }

    @Test
    @DisplayName("Shopee 物流轨迹按时间升序映射")
    void mapsTrackingEventsInAscendingTime() throws Exception {
        enqueueJson(orderDetailJson("SHIPPED"));
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-tracking-info",
              "response": {
                "order_sn": "SN1",
                "package_number": "PKG1",
                "logistics_status": "LOGISTICS_DELIVERY_DONE",
                "tracking_info": [
                  {"update_time": 1700000500, "description": "Delivered", "logistics_status": "DELIVERED"},
                  {"update_time": 1700000000, "description": "Picked up", "logistics_status": "PICKED_UP"}
                ]
              }
            }
            """);
        enqueueJson("""
            {
              "error": "",
              "message": "",
              "request_id": "req-tracking-number",
              "response": {"tracking_number": "TRK1"}
            }
            """);

        UnifiedShipment shipment = newAdapter().getTrackingEvents(authContext, "SN1");

        assertEquals("PKG1", shipment.getShipmentId());
        assertEquals("TRK1", shipment.getTrackingNumber());
        assertEquals("LOGISTICS_DELIVERY_DONE", shipment.getOriginalStatus());
        assertEquals(2, shipment.getTrackingEvents().size());
        assertEquals("Picked up", shipment.getTrackingEvents().get(0).getDescription());
        assertEquals(Instant.ofEpochSecond(1700000000L), shipment.getTrackingEvents().get(0).getTime());
        assertEquals("Delivered", shipment.getTrackingEvents().get(1).getDescription());
    }

    private ShopeeAdapter newAdapter() throws Exception {
        ShopeeAdapter adapter = new ShopeeAdapter();
        setField(adapter, "shopeeApiClient", client);
        setField(adapter, "shippingDocumentPollAttempts", 2);
        setField(adapter, "shippingDocumentPollIntervalMs", 0L);
        return adapter;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void enqueueJson(String body) {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body));
    }

    private RecordedRequest takeRequest() throws Exception {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request, "应该收到一次 Shopee API 请求");
        return request;
    }

    private JsonNode readBody(RecordedRequest request) throws Exception {
        return objectMapper.readTree(request.getBody().readUtf8());
    }

    private void assertPublicSignature(RecordedRequest request, String path) {
        String timestamp = request.getRequestUrl().queryParameter("timestamp");
        assertNotNull(timestamp);
        assertEquals(String.valueOf(PARTNER_ID), request.getRequestUrl().queryParameter("partner_id"));
        assertEquals(client.signPublic(path, Long.parseLong(timestamp)), request.getRequestUrl().queryParameter("sign"));
    }

    private void assertShopSignature(RecordedRequest request, String path) {
        String timestamp = request.getRequestUrl().queryParameter("timestamp");
        assertNotNull(timestamp);
        assertEquals(String.valueOf(PARTNER_ID), request.getRequestUrl().queryParameter("partner_id"));
        assertEquals(authContext.getAccessToken(), request.getRequestUrl().queryParameter("access_token"));
        assertEquals(authContext.getShopId(), request.getRequestUrl().queryParameter("shop_id"));
        assertEquals(client.signShop(path, Long.parseLong(timestamp), authContext.getAccessToken(), authContext.getShopId()), request.getRequestUrl().queryParameter("sign"));
    }

    private String orderDetailJson(String status) {
        return String.format("""
            {
              "error": "",
              "message": "",
              "request_id": "req-detail",
              "response": {
                "order_list": [
                  {
                    "order_sn": "SN1",
                    "region": "SG",
                    "currency": "SGD",
                    "total_amount": 12.34,
                    "order_status": "%s",
                    "shipping_carrier": "Shopee Express",
                    "create_time": 1700000000,
                    "update_time": 1700003600,
                    "buyer_user_id": 9001,
                    "buyer_username": "buyer-1",
                    "recipient_address": {
                      "name": "Alice",
                      "phone": "123456",
                      "region": "SG",
                      "state": "Central",
                      "city": "Singapore",
                      "district": "Downtown",
                      "zipcode": "018956",
                      "full_address": "1 Marina Blvd"
                    },
                    "item_list": [
                      {
                        "item_id": 111,
                        "item_name": "Keyboard",
                        "model_id": 222,
                        "model_name": "Blue Switch",
                        "model_sku": "SKU-BLUE",
                        "model_quantity_purchased": 2,
                        "model_discounted_price": 6.17,
                        "order_item_id": 333,
                        "image_info": {"image_url": "https://example.com/image.jpg"}
                      }
                    ],
                    "package_list": [
                      {
                        "package_number": "PKG1",
                        "logistics_status": "LOGISTICS_READY",
                        "shipping_carrier": "SPX Express"
                      }
                    ]
                  }
                ]
              }
            }
            """, status);
    }

    private String emptyOrderDetailJson() {
        return """
            {
              "error": "",
              "message": "",
              "request_id": "req-detail",
              "response": {"order_list": []}
            }
            """;
    }

    private String hmac(String key, String baseString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
