# Ecommerce4j SDK 

![Java](https://img.shields.io/badge/Java-17%2B-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-green) ![License](https://img.shields.io/badge/License-MIT-yellow) ![Status](https://img.shields.io/badge/Status-Educational%20Only-red)

> 🚨 **免责声明 / DISCLAIMER**
>
> **本项目仅供学习与架构参考，严禁直接引入生产环境！**
> **This project is for educational purposes only. DO NOT use in production.**
>
> 本项目是作者基于电商ERP业务场景构建的架构验证（PoC），**不具备**通用性与长期维护性。由于跨境电商平台 API 变动频繁，直接使用本 SDK 极大概率会导致生产事故。作者不对任何因使用本项目造成的损失（包括但不限于订单丢失、资金损失）承担责任。

**Ecommerce4j SDK** 是一个轻量级、统一的海外电商平台集成 SDK。旨在通过一套标准化的接口（Unified Interface），展示如何屏蔽不同电商平台（TikTok Shop, Mercado Libre 等）的 API 差异。

> 💡 **项目核心价值**
>
> 本项目适合作为 **Reference Architecture (参考架构)**，帮助开发者理解：
> * 如何设计统一的领域模型 (Unified Domain Model)
> * 如何使用适配器模式 (Adapter Pattern) 处理异构系统
> * 如何处理复杂的业务场景（如 Meli 合单、OAuth2 自动刷新）
>
> *建议阅读源码理解设计思路后，根据贵公司的实际业务需求进行二次开发。*
---

## ✨ 核心特性

- **统一领域模型**：通过 `UnifiedOrder`, `UnifiedShipment` 等对象，抹平了不同平台的字段差异。
- **开箱即用**：深度集成 Spring Boot，通过 Starter 方式自动装配。
- **扩展性强**：基于适配器模式设计，新增平台只需实现标准接口，无需改动上层业务代码。
- **目前支持平台**：
  - [x] **TikTok Shop** (Order, Fulfillment, Logistics, Auth)
  - [x] **Mercado Libre** (Order, Fulfillment, Logistics, Auth)

## 🧩 功能模块 (Core Capabilities)

本项目覆盖了跨境电商 ERP 最核心的三大业务域：

### 1. 🔐 店铺授权 (Authorization)
- **标准 OAuth2 流程**：封装了 `code` 换取 `token` 的标准逻辑。
- **自动刷新**：提供 `refreshTokens` 接口，支持 Access Token 过期后的无感刷新。
- **店铺信息同步**：获取店铺基础信息（ID, 名称, 地区），自动适配不同平台的字段命名。

### 2. 📦 订单管理 (Order Management)
- **统一查询**：通过 `OrderQuery` 对象支持按时间、状态分页查询订单。
- **详情拉取**：自动补全商品图片、SKU 信息，处理货币金额精度。
- **状态归一化**：将各平台的几十种异构状态映射为标准的 `UNIFIED_Status`。

### 3. 🚚 物流履约 (Logistics & Fulfillment)
- **面单获取**：支持下载 PDF 格式的物流面单，自动处理 Base64 解码。
- **发货回填**：支持卖家自发货模式下回填 Tracking Number。
- **轨迹追踪**：统一的轨迹节点查询，自动解析时区和时间格式。

## 🛠️ 快速开始

### 1. 引入依赖
（假设你已经将项目 install 到本地仓库）
```xml
<dependency>
    <groupId>com.ecommerce4j</groupId>
    <artifactId>ecommerce4j-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置参数
在 `application.yml` 中配置对应平台的密钥：

```yaml
tiktok:
  app_key: "YOUR_TIKTOK_APP_KEY"
  app_secret: "YOUR_TIKTOK_APP_SECRET"
  auth_url: "https://auth.tiktok-shops.com/oauth/authorize"

mercadolibre:
  app_id: "YOUR_ML_APP_ID"
  secret_key: "YOUR_ML_SECRET_KEY"
  auth_url: "https://auth.mercadolibre.com/authorization"
```

### 3. 使用 SDK
SDK 会自动注入 `PlatformFactory`，你可以通过它获取任意平台的标准服务：

```java
@Autowired
private PlatformFactory platformFactory;

public void syncOrders() {
  // 1. 构建AuthContext统一认证上下文
  Platform platform = Platform.TIKTOK_SHOP;
  AuthContext authContext = this.getAuthContextForSeller("sellerId", platform);

  // 2. 准备订单查询参数 (OrderQuery)
  // 我们想要获取过去30天内创建的、状态为“待发货”的订单。
  OrderQuery query = OrderQuery.builder()
          .createTimeFrom(Instant.now().minus(30, ChronoUnit.DAYS))
          .createTimeTo(Instant.now())
          .pageSize(1)
          .build();
    
  // 获取店铺授权url
  String uuid = UUID.randomUUID().toString(true);
  EcommAuthorizationService authorizationService = platformFactory.getAuthorizationService(platform);
  String authorizationUrl = authorizationService.getAuthorizationUrl(uuid);  
 
  // 获取店铺信息  
  UnifiedShopInfo shopInfo = authorizationService.getShopInfo(authContext);

  // 调用获取订单列表
  EcommOrderService orderService = platformFactory.getOrderService(platform);  
  PaginatedResult<UnifiedOrder> orders = orderService.getOrders(authContext, query);
  List<UnifiedOrder> data = orders.getData();
  log.info("成功从平台 [{}] 获取到 {} 条订单。", platform.getDescription(), data.size());

  // 获取物流面单
  EcommFulfillmentService fulfillmentService = platformFactory.getFulfillmentService(platform);  
  FulfillmentAction fulfillmentAction = fulfillmentService.prepareFulfillment(authContext, "shipmentId");

  // 获取物流信息
  EcommLogisticsService logisticsService = platformFactory.getLogisticsService(platform);
  UnifiedShipment trackingEvents = logisticsService.getTrackingEvents(authContext, "shipmentId");
}

/**
 * 从您的数据库或缓存中获取卖家的授权凭证。
 *
 * @param sellerId 卖家ID
 * @param platform 平台
 * @return 持久化的 AuthContext 对象
 */
private AuthContext getAuthContextForSeller(String sellerId, Platform platform) {
  // log.info("【模拟】正在从数据库为卖家 {} 查找平台 {} 的授权信息...", sellerId, platform.name());
  // 在这里，您应该执行类似 "SELECT * FROM auth_tokens WHERE seller_id = ? AND platform = ?" 的查询
  if (Platform.TIKTOK_SHOP.equals(platform)) {
    return AuthContext.builder()
            .platform(Platform.TIKTOK_SHOP)
            .accessToken("accessToken") // 从数据库读取
            .refreshToken("refreshToken") // 从数据库读取
            .accessTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS)) // 从数据库读取
            .build();
  } else if (Platform.MERCADO_LIBRE.equals(platform)) {
    return AuthContext.builder()
            .platform(Platform.MERCADO_LIBRE)
            .accessToken("accessToken") // 从数据库读取
            .refreshToken("refreshToken") // 从数据库读取
            .accessTokenExpiresAt(Instant.now().plus(6, ChronoUnit.HOURS)) // 从数据库读取
            .sellerId("sellerId") // MercadoLibre 使用 sellerId
            .build();
  }
  return null;
}
```

## � 核心亮点 (Key Features)

为了解决跨境电商场景下的复杂痛点，本项目在适配器层做了大量深度封装：

- **Mercado Libre 合单智能处理 ("Pack" Logic)**
  - 在 Mercado Libre 中，多个订单可能会被合并为一个包裹 (`Pack`) 发货。
  - SDK 的 `MercadoLibreAdapter` 会自动识别 `Pack` 类型的订单，**递归抓取包内所有子订单**，并将它们聚合成一个包含完整商品明细的 `UnifiedOrder` 返回。
  - 开发者无需关心它是单品单还是合单，收到的永远是结构一致的订单对象。

- **物流单号“双模探测” (`resolveShipmentId`)**
  - 获取面单时，Meli 的 API 需要区分传入的是 Order ID 还是 Shipment ID 还是 Pack ID。
  - SDK 内部实现了**双模探测机制**：自动尝试将 ID 解析为普通订单或 Pack 合单，智能定位到正确的 `shipping_id`，极大降低了上层业务的调用复杂度。

- **统一状态机映射 (Unified Status)**
  - 不同平台的状态机完全不同（如 TikTok 的 `AWAITING_SHIPMENT` vs Mercado 的 `ready_to_ship`）。
  - SDK 内部维护了一套标准状态机，所有平台的原始状态都会被**自动映射**为标准状态（如 `UNIFIED_WAIT_SHIP`），确保业务逻辑的一致性。

- **全链路 API 日志增强**
  - 内置 `ApiLoggingInterceptor`，对所有进出的 HTTP 请求进行审计。
  - 实现了**智能截断**（对过长的 Base64 面单数据进行截断）和**敏感头过滤**，确保日志既具备排查价值又不会撑爆存储。

## �🏗️ 架构设计

项目采用典型的 **适配器模式 (Adapter Pattern)**：

- **`EcommOrderService`**: 定义标准的订单操作（查单、详情）。
- **`AbstractAdapter`**: 封装 OkHttp 客户端、JSON 序列化、签名算法等通用逻辑。
- **`TikTokShopAdapter` / `MercadoLibreAdapter`**: 实现具体平台的 API 调用和数据映射。

## 🤝 贡献与反馈 (Contribution)

**本项目旨在探索跨境电商集成的最佳架构实践。**

由于各电商平台的 API 迭代频繁，本 SDK 更侧重于**通用适配逻辑**与**领域模型**的稳定性，而非追求对特定 API 版本的实时覆盖。

- **二次开发**：项目已预留了扩展接口，建议您 Fork 本仓库，基于此架构快速对接您所需的特定业务平台。
- **共建生态**：如果您修复了核心逻辑 Bug 或完成了新平台的适配，**热烈欢迎提交 PR**！
- **支持作者**：如果这个项目的**设计思路**为您带来了灵感，请不吝点个 **⭐️ Star** 支持一下！您的鼓励是我持续分享的动力。
