package com.ecommerce4j.api.platform.shopee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shopee OpenAPI v2
 */
public final class ShopeeModels {

    private ShopeeModels() {
    }

    /**
     * 授权 token 响应
     */
    @Data
    public static class TokenResponse extends ShopeeResponse {

        /**
         * 应用 Partner ID
         */
        @JsonProperty("partner_id")
        private Long partnerId;

        /**
         * 单店铺授权或刷新时返回的店铺 ID
         */
        @JsonProperty("shop_id")
        private Long shopId;

        /**
         * 跨境主账号刷新时返回的商家 ID
         */
        @JsonProperty("merchant_id")
        private Long merchantId;

        /**
         * 主账号授权时返回的店铺 ID 列表
         */
        @JsonProperty("shop_id_list")
        private List<Long> shopIdList;

        /**
         * 主账号授权时返回的商家 ID 列表
         */
        @JsonProperty("merchant_id_list")
        private List<Long> merchantIdList;

        /**
         * SCS 场景授权时返回的供应商 ID 列表
         */
        @JsonProperty("supplier_id_list")
        private List<Long> supplierIdList;

        /**
         * 用户型应用授权时返回的用户 ID 列表
         */
        @JsonProperty("user_id_list")
        private List<Long> userIdList;

        /**
         * 调用店铺级 API 的访问令牌
         */
        @JsonProperty("access_token")
        private String accessToken;

        /**
         * 用于刷新 access_token 的一次性刷新令牌
         */
        @JsonProperty("refresh_token")
        private String refreshToken;

        /**
         * access_token 有效期，官方通常返回秒数
         */
        @JsonProperty("expire_in")
        private Long expireIn;
    }

    /**
     * 店铺基础信息响应
     */
    @Data
    public static class ShopInfoResponse extends ShopeeResponse {

        /**
         * 店铺名称
         */
        @JsonProperty("shop_name")
        private String shopName;

        /**
         * 店铺所在市场，例如 SG、MY、BR
         */
        private String region;

        /**
         * 店铺状态
         */
        private String status;

        /**
         * 店铺授权开始时间，Unix 秒
         */
        @JsonProperty("auth_time")
        private Long authTime;

        /**
         * 店铺授权过期时间，Unix 秒
         */
        @JsonProperty("expire_time")
        private Long expireTime;

        /**
         * 是否跨境店铺
         */
        @JsonProperty("is_cb")
        private Boolean cb;

        /**
         * 是否 SIP 店铺
         */
        @JsonProperty("is_sip")
        private Boolean sip;

        /**
         * 店铺所属商家 ID
         */
        @JsonProperty("merchant_id")
        private Long merchantId;

        /**
         * 店铺履约类型标识
         */
        @JsonProperty("shop_fulfillment_flag")
        private String shopFulfillmentFlag;

        /**
         * 是否主店铺
         */
        @JsonProperty("is_main_shop")
        private Boolean mainShop;

        /**
         * 是否直连店铺
         */
        @JsonProperty("is_direct_shop")
        private Boolean directShop;
    }

    /**
     * 订单列表响应
     */
    @Data
    public static class OrderListResponse extends ShopeeResponse {

        /**
         * 订单列表响应主体
         */
        private OrderListData response;
    }

    /**
     * 订单列表分页数据
     */
    @Data
    public static class OrderListData {

        /**
         * 是否还有下一页
         */
        private Boolean more;

        /**
         * 下一页游标
         */
        @JsonProperty("next_cursor")
        private String nextCursor;

        /**
         * 订单摘要列表
         */
        @JsonProperty("order_list")
        private List<OrderSummary> orderList;
    }

    /**
     * 订单摘要
     */
    @Data
    public static class OrderSummary {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 原始订单状态
         */
        @JsonProperty("order_status")
        private String orderStatus;

        /**
         * 高阶履约订单对应的 booking 编号
         */
        @JsonProperty("booking_sn")
        private String bookingSn;
    }

    /**
     * 订单详情响应
     */
    @Data
    public static class OrderDetailResponse extends ShopeeResponse {

        /**
         * 订单详情响应主体
         */
        private OrderDetailData response;

        /**
         * 平台返回的警告信息
         */
        private List<String> warning;
    }

    /**
     * 订单详情数据
     */
    @Data
    public static class OrderDetailData {

        /**
         * 订单详情列表
         */
        @JsonProperty("order_list")
        private List<Order> orderList;
    }

    /**
     * Shopee 订单详情
     */
    @Data
    public static class Order {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * 下单市场
         */
        private String region;

        /**
         * 订单币种
         */
        private String currency;

        /**
         * 是否货到付款订单
         */
        private Boolean cod;

        /**
         * 买家实付订单总额
         */
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;

        /**
         * Shopee 原始订单状态
         */
        @JsonProperty("order_status")
        private String orderStatus;

        /**
         * 买家选择的物流承运商
         */
        @JsonProperty("shipping_carrier")
        private String shippingCarrier;

        /**
         * 支付方式
         */
        @JsonProperty("payment_method")
        private String paymentMethod;

        /**
         * 买家给卖家的留言
         */
        @JsonProperty("message_to_seller")
        private String messageToSeller;

        /**
         * 订单创建时间，Unix 秒
         */
        @JsonProperty("create_time")
        private Long createTime;

        /**
         * 订单更新时间，Unix 秒
         */
        @JsonProperty("update_time")
        private Long updateTime;

        /**
         * 买家用户 ID
         */
        @JsonProperty("buyer_user_id")
        private Long buyerUserId;

        /**
         * 买家昵称
         */
        @JsonProperty("buyer_username")
        private String buyerUsername;

        /**
         * 收货地址
         */
        @JsonProperty("recipient_address")
        private RecipientAddress recipientAddress;

        /**
         * 商品明细列表
         */
        @JsonProperty("item_list")
        private List<OrderItem> itemList;

        /**
         * 包裹列表
         */
        @JsonProperty("package_list")
        private List<PackageInfo> packageList;
    }

    /**
     * 收货地址
     */
    @Data
    public static class RecipientAddress {

        /**
         * 收件人姓名
         */
        private String name;

        /**
         * 收件人电话
         */
        private String phone;

        /**
         * 镇/乡
         */
        private String town;

        /**
         * 区/县
         */
        private String district;

        /**
         * 城市
         */
        private String city;

        /**
         * 州/省
         */
        private String state;

        /**
         * 国家或地区编码
         */
        private String region;

        /**
         * 邮编
         */
        private String zipcode;

        /**
         * 完整地址
         */
        @JsonProperty("full_address")
        private String fullAddress;
    }

    /**
     * Shopee 订单商品行
     */
    @Data
    public static class OrderItem {

        /**
         * 商品 ID
         */
        @JsonProperty("item_id")
        private Long itemId;

        /**
         * 商品名称
         */
        @JsonProperty("item_name")
        private String itemName;

        /**
         * 卖家设置的商品 SKU
         */
        @JsonProperty("item_sku")
        private String itemSku;

        /**
         * 规格模型 ID
         */
        @JsonProperty("model_id")
        private Long modelId;

        /**
         * 规格名称
         */
        @JsonProperty("model_name")
        private String modelName;

        /**
         * 卖家设置的规格 SKU
         */
        @JsonProperty("model_sku")
        private String modelSku;

        /**
         * 购买数量
         */
        @JsonProperty("model_quantity_purchased")
        private Integer modelQuantityPurchased;

        /**
         * 折后单价
         */
        @JsonProperty("model_discounted_price")
        private BigDecimal modelDiscountedPrice;

        /**
         * 原始单价
         */
        @JsonProperty("model_original_price")
        private BigDecimal modelOriginalPrice;

        /**
         * Shopee 订单行 ID
         */
        @JsonProperty("order_item_id")
        private Long orderItemId;

        /**
         * 商品图片信息
         */
        @JsonProperty("image_info")
        private ImageInfo imageInfo;
    }

    /**
     * 商品图片信息
     */
    @Data
    public static class ImageInfo {

        /**
         * 商品主图 URL
         */
        @JsonProperty("image_url")
        private String imageUrl;
    }

    /**
     * Shopee 包裹信息
     */
    @Data
    public static class PackageInfo {

        /**
         * Shopee 包裹号，履约和面单接口均使用该值
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * 包裹物流状态
         */
        @JsonProperty("logistics_status")
        private String logisticsStatus;

        /**
         * 包裹承运商
         */
        @JsonProperty("shipping_carrier")
        private String shippingCarrier;

        /**
         * Shopee 物流渠道 ID
         */
        @JsonProperty("logistics_channel_id")
        private Long logisticsChannelId;

        /**
         * 包裹内商品行
         */
        @JsonProperty("item_list")
        private List<PackageItem> itemList;
    }

    /**
     * 包裹内商品行
     */
    @Data
    public static class PackageItem {

        /**
         * 商品 ID
         */
        @JsonProperty("item_id")
        private Long itemId;

        /**
         * 规格模型 ID
         */
        @JsonProperty("model_id")
        private Long modelId;

        /**
         * 包裹内该规格数量
         */
        @JsonProperty("model_quantity")
        private Integer modelQuantity;

        /**
         * Shopee 订单行 ID
         */
        @JsonProperty("order_item_id")
        private Long orderItemId;

        /**
         * 商品库存位置 ID
         */
        @JsonProperty("product_location_id")
        private String productLocationId;
    }

    /**
     * 履约参数响应
     */
    @Data
    public static class ShippingParameterResponse extends ShopeeResponse {

        /**
         * 履约参数响应主体
         */
        private ShippingParameterData response;
    }

    /**
     * 履约参数数据
     */
    @Data
    public static class ShippingParameterData {

        /**
         * 本次发货需要卖家提供哪些信息
         */
        @JsonProperty("info_needed")
        private InfoNeeded infoNeeded;

        /**
         * Dropoff 可选参数
         */
        private DropoffParameter dropoff;

        /**
         * Pickup 可选参数
         */
        private PickupParameter pickup;
    }

    /**
     * 平台要求的履约信息标记
     */
    @Data
    public static class InfoNeeded {

        /**
         * 是否需要 dropoff 参数
         */
        private Boolean dropoff;

        /**
         * 是否需要 pickup 参数
         */
        private Boolean pickup;

        /**
         * 是否需要 non-integrated 自有物流参数
         */
        @JsonProperty("non_integrated")
        private Boolean nonIntegrated;
    }

    /**
     * Dropoff 参数集合
     */
    @Data
    public static class DropoffParameter {

        /**
         * 可投递网点列表
         */
        @JsonProperty("branch_list")
        private List<Branch> branchList;

        /**
         * 可投递 slug 列表
         */
        @JsonProperty("slug_list")
        private List<Slug> slugList;
    }

    /**
     * Dropoff 网点
     */
    @Data
    public static class Branch {

        /**
         * 网点 ID
         */
        @JsonProperty("branch_id")
        private Long branchId;

        /**
         * 国家或地区编码
         */
        private String region;

        /**
         * 州/省
         */
        private String state;

        /**
         * 城市
         */
        private String city;

        /**
         * 区/县
         */
        private String district;

        /**
         * 镇/乡
         */
        private String town;

        /**
         * 网点地址
         */
        private String address;

        /**
         * 邮编
         */
        private String zipcode;
    }

    /**
     * Dropoff slug 参数
     */
    @Data
    public static class Slug {

        /**
         * slug 编码
         */
        private String slug;

        /**
         * slug 展示名称
         */
        @JsonProperty("slug_name")
        private String slugName;
    }

    /**
     * Pickup 参数集合
     */
    @Data
    public static class PickupParameter {

        /**
         * 可上门揽收地址列表
         */
        @JsonProperty("address_list")
        private List<PickupAddress> addressList;
    }

    /**
     * 上门揽收地址
     */
    @Data
    public static class PickupAddress {

        /**
         * 揽收地址 ID
         */
        @JsonProperty("address_id")
        private Long addressId;

        /**
         * 国家或地区编码
         */
        private String region;

        /**
         * 州/省
         */
        private String state;

        /**
         * 城市
         */
        private String city;

        /**
         * 区/县
         */
        private String district;

        /**
         * 镇/乡
         */
        private String town;

        /**
         * 详细地址
         */
        private String address;

        /**
         * 邮编
         */
        private String zipcode;

        /**
         * 地址标记
         */
        @JsonProperty("address_flag")
        private List<String> addressFlag;

        /**
         * 可选揽收时间段
         */
        @JsonProperty("time_slot_list")
        private List<PickupTimeSlot> timeSlotList;
    }

    /**
     * 揽收时间段
     */
    @Data
    public static class PickupTimeSlot {

        /**
         * 揽收日期，Unix 秒
         */
        private Long date;

        /**
         * 揽收时间展示文本
         */
        @JsonProperty("time_text")
        private String timeText;

        /**
         * 揽收时间段 ID
         */
        @JsonProperty("pickup_time_id")
        private String pickupTimeId;

        /**
         * 时间段标记
         */
        private List<String> flags;
    }

    /**
     * 发货请求
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShipOrderRequest {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * Pickup 发货参数
         */
        private PickupShipParameter pickup;

        /**
         * Dropoff 发货参数
         */
        private DropoffShipParameter dropoff;

        /**
         * 自有物流发货参数
         */
        @JsonProperty("non_integrated")
        private NonIntegratedShipParameter nonIntegrated;
    }

    /**
     * Pickup 发货参数
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PickupShipParameter {

        /**
         * 揽收地址 ID
         */
        @JsonProperty("address_id")
        private Long addressId;

        /**
         * 揽收时间段 ID
         */
        @JsonProperty("pickup_time_id")
        private String pickupTimeId;

        /**
         * 可选运单号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;
    }

    /**
     * Dropoff 发货参数
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DropoffShipParameter {

        /**
         * 投递网点 ID
         */
        @JsonProperty("branch_id")
        private Long branchId;

        /**
         * 寄件人真实姓名
         */
        @JsonProperty("sender_real_name")
        private String senderRealName;

        /**
         * 可选运单号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;

        /**
         * Dropoff slug
         */
        private String slug;
    }

    /**
     * 自有物流发货参数
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NonIntegratedShipParameter {

        /**
         * 自有物流运单号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;
    }

    /**
     * 运单号响应
     */
    @Data
    public static class TrackingNumberResponse extends ShopeeResponse {

        /**
         * 运单号响应主体
         */
        private TrackingNumberData response;
    }

    /**
     * 运单号数据
     */
    @Data
    public static class TrackingNumberData {

        /**
         * 平台物流运单号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;

        /**
         * 首公里运单号
         */
        @JsonProperty("first_mile_tracking_number")
        private String firstMileTrackingNumber;
    }

    /**
     * 面单参数响应
     */
    @Data
    public static class ShippingDocumentParameterResponse extends ShopeeResponse {

        /**
         * 面单参数响应主体
         */
        private ShippingDocumentParameterData response;

        /**
         * 面单参数警告列表
         */
        private List<DocumentWarning> warning;
    }

    /**
     * 面单参数数据
     */
    @Data
    public static class ShippingDocumentParameterData {

        /**
         * 面单参数结果列表
         */
        @JsonProperty("result_list")
        private List<ShippingDocumentParameterResult> resultList;
    }

    /**
     * 单个包裹的面单参数结果
     */
    @Data
    public static class ShippingDocumentParameterResult {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * 平台建议的面单类型
         */
        @JsonProperty("suggest_shipping_document_type")
        private String suggestShippingDocumentType;

        /**
         * 可选择的面单类型列表
         */
        @JsonProperty("selectable_shipping_document_type")
        private List<String> selectableShippingDocumentType;

        /**
         * 失败错误码
         */
        @JsonProperty("fail_error")
        private String failError;

        /**
         * 失败错误描述
         */
        @JsonProperty("fail_message")
        private String failMessage;
    }

    /**
     * 面单接口中的订单包裹引用
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentOrder {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * 创建面单时可选的运单号
         */
        @JsonProperty("tracking_number")
        private String trackingNumber;

        /**
         * 面单类型
         */
        @JsonProperty("shipping_document_type")
        private String shippingDocumentType;
    }

    /**
     * 面单警告信息
     */
    @Data
    public static class DocumentWarning {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;
    }

    /**
     * 面单创建或查询结果响应
     */
    @Data
    public static class ShippingDocumentOperationResponse extends ShopeeResponse {

        /**
         * 面单操作响应主体
         */
        private ShippingDocumentOperationData response;

        /**
         * 面单操作警告列表
         */
        private List<DocumentWarning> warning;
    }

    /**
     * 面单操作数据
     */
    @Data
    public static class ShippingDocumentOperationData {

        /**
         * 面单操作结果列表
         */
        @JsonProperty("result_list")
        private List<ShippingDocumentOperationResult> resultList;
    }

    /**
     * 单个包裹的面单操作结果
     */
    @Data
    public static class ShippingDocumentOperationResult {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * 面单任务状态，例如 READY
         */
        private String status;

        /**
         * 失败错误码
         */
        @JsonProperty("fail_error")
        private String failError;

        /**
         * 失败错误描述
         */
        @JsonProperty("fail_message")
        private String failMessage;
    }

    /**
     * 物流轨迹响应
     */
    @Data
    public static class TrackingInfoResponse extends ShopeeResponse {

        /**
         * 物流轨迹响应主体
         */
        private TrackingInfoData response;
    }

    /**
     * 物流轨迹数据
     */
    @Data
    public static class TrackingInfoData {

        /**
         * Shopee 订单号
         */
        @JsonProperty("order_sn")
        private String orderSn;

        /**
         * Shopee 包裹号
         */
        @JsonProperty("package_number")
        private String packageNumber;

        /**
         * 包裹当前物流状态
         */
        @JsonProperty("logistics_status")
        private String logisticsStatus;

        /**
         * 正向物流轨迹列表
         */
        @JsonProperty("tracking_info")
        private List<TrackingEvent> trackingInfo;

        /**
         * 自提 PIN 码
         */
        @JsonProperty("collection_pin_code")
        private String collectionPinCode;

        /**
         * 逆向物流运单号
         */
        @JsonProperty("reversed_tracking_number")
        private String reversedTrackingNumber;

        /**
         * 逆向物流承运商
         */
        @JsonProperty("reversed_courier_name")
        private String reversedCourierName;
    }

    /**
     * 单个物流轨迹节点
     */
    @Data
    public static class TrackingEvent {

        /**
         * 轨迹更新时间，Unix 秒
         */
        @JsonProperty("update_time")
        private Long updateTime;

        /**
         * 轨迹描述
         */
        private String description;

        /**
         * 该节点的物流状态
         */
        @JsonProperty("logistics_status")
        private String logisticsStatus;

        /**
         * 退货相关节点的返回码
         */
        @JsonProperty("return_code")
        private String returnCode;
    }
}
