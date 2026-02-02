# Ecommerce4j SDK 

![Java](https://img.shields.io/badge/Java-17%2B-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-green) ![License](https://img.shields.io/badge/License-MIT-yellow)

**Ecommerce4j SDK** 是一个轻量级、统一的海外电商平台集成 SDK。旨在通过一套标准化的接口（Unified Interface），屏蔽不同电商平台（TikTok Shop, Mercado Libre 等）的 API 差异，帮助开发者快速构建跨境电商 ERP 或中间件系统。

> 💡 **项目说明 / Note**
> 
> 本项目沉淀了作者在跨境电商业务中的架构思考，重点在于**统一领域模型的设计**与**多平台适配器模式的实践**。
> 
> - **定位**：作为轻量级中间件架构的 Concept Proof (PoC) 实现。
> - **现状**：核心链路已调通，具备灵活扩展新平台的能力。
> - **计划**：目前测试用例尚需完善（官方文档存在多数特殊情况未列出，需要实际接入测试），后续计划不断提升覆盖率，以满足生产级交付标准。
> 
> *仅供学习参考，生产环境集成建议基于此架构进行二次开发与验证。*

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
    // 1. 获取 TikTok 的订单服务
    EcommOrderService tiktokOrderService = platformFactory.getOrderService(Platform.TIKTOK_SHOP);

    // 2. 构建统一查询参数
    OrderQuery query = new OrderQuery();
    query.setPageSize(20);
    query.setCreateTimeFrom(Instant.now().minus(7, ChronoUnit.DAYS));

    // 3. 获取标准化的订单列表
    AuthContext auth = new AuthContext("ACCESS_TOKEN", ...);
    PaginatedResult<UnifiedOrder> result = tiktokOrderService.getOrders(auth, query);

    // 4. 处理订单（无需关心是 TikTok 还是 Mercado）
    result.getData().forEach(order -> {
        System.out.println("订单号: " + order.getOrderId());
        System.out.println("金额: " + order.getTotalAmount());
    });
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

## 🤝 贡献与反馈

这是一个开源的“玩具”项目，如果你在使用过程中发现了 Bug，或者是想添加新的平台支持，非常欢迎 Submit Pull Request！

让我们一起把这个轮子造得更圆！🚀
