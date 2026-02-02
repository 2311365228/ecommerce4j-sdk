package com.ecommerce4j.core;

import com.ecommerce4j.api.EcommAuthorizationService;
import com.ecommerce4j.api.EcommFulfillmentService;
import com.ecommerce4j.api.EcommLogisticsService;
import com.ecommerce4j.api.EcommOrderService;
import com.ecommerce4j.api.enums.Platform;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformFactory {

    // 注入所有实现了相应接口的Bean列表
    private final List<EcommAuthorizationService> authServiceList;
    private final List<EcommOrderService> orderServiceList;
    private final List<EcommFulfillmentService> fulfillmentServiceList;
    private final List<EcommLogisticsService> logisticsServiceList;

    // 存储最终的、类型安全的Map
    private Map<Platform, EcommAuthorizationService> authServices;
    private Map<Platform, EcommOrderService> orderServices;
    private Map<Platform, EcommFulfillmentService> fulfillmentServices;
    private Map<Platform, EcommLogisticsService> logisticsServices;

    // 注入 ApplicationContext 以便获取Bean的名称
    private final ApplicationContext applicationContext;

    @Autowired
    public PlatformFactory(List<EcommAuthorizationService> authServiceList,
                           List<EcommOrderService> orderServiceList,
                           List<EcommFulfillmentService> fulfillmentServiceList,
                           List<EcommLogisticsService> logisticsServiceList,
                           ApplicationContext applicationContext) {
        this.authServiceList = authServiceList;
        this.orderServiceList = orderServiceList;
        this.fulfillmentServiceList = fulfillmentServiceList;
        this.logisticsServiceList = logisticsServiceList;
        this.applicationContext = applicationContext;
    }

    /**
     * 使用 @PostConstruct 注解，在所有依赖注入完成后，执行这个初始化方法。
     * 这里将完成从 List 到 Map 的转换。
     */
    @PostConstruct
    public void init() {
        this.orderServices = buildServiceMap(orderServiceList);
        this.authServices = buildServiceMap(authServiceList);
        this.fulfillmentServices = buildServiceMap(fulfillmentServiceList);
        this.logisticsServices = buildServiceMap(logisticsServiceList);
    }

    /**
     * 通用的转换方法，将服务列表转换为 Platform -> Service 的 Map。
     * @param serviceList Spring注入的服务列表
     * @param <T> 服务接口类型
     * @return 组装好的Map
     */
    private <T> Map<Platform, T> buildServiceMap(List<T> serviceList) {
        if (serviceList == null || serviceList.isEmpty()) {
            return new EnumMap<>(Platform.class);
        }

        // 使用 ApplicationContext 来反向查找每个Bean实例的名称
        // 然后将 Bean 名称转换为 Platform 枚举
        return serviceList.stream()
            .collect(Collectors.toMap(
                this::findPlatformByBean, // Key: Platform 枚举
                Function.identity(), // Value: 服务实例本身
                (existing, replacement) -> existing, // 合并函数，如果key重复则保留现有的
                () -> new EnumMap<>(Platform.class) // 指定Map的类型为EnumMap，性能更高
            ));
    }

    /**
     * 根据Bean的实例，反向查找它在Spring容器中的名称，并转换为Platform枚举。
     * @param serviceBean Bean实例
     * @return 对应的Platform枚举
     * @throws IllegalStateException 如果找不到Bean的名称或名称无法匹配任何Platform
     */
    private Platform findPlatformByBean(Object serviceBean) {
        // 获取该Bean实例在Spring容器中所有可能的名称
        String[] beanNames = applicationContext.getBeanNamesForType(serviceBean.getClass());
        if (beanNames == null || beanNames.length == 0) {
            throw new IllegalStateException("无法找到Bean " + serviceBean.getClass().getName() + " 在Spring容器中的名称。");
        }

        // 遍历所有可能的名称，尝试匹配Platform枚举
        for (String beanName : beanNames) {
            try {
                // 我们之前将Bean命名为 "TIKTOK_SHOP", "MERCADO_LIBRE"
                // Platform.valueOf() 可以将字符串转换为枚举
                return Platform.valueOf(beanName);
            } catch (IllegalArgumentException e) {
                // 如果这个beanName不是一个有效的Platform枚举名，忽略并继续尝试下一个
            }
        }

        // 如果所有名称都无法匹配
        throw new IllegalStateException("Bean " + serviceBean.getClass().getName() + " 的名称 " + String.join(",", beanNames) + " 无法匹配任何一个Platform枚举值。");
    }

    public EcommAuthorizationService getAuthorizationService(Platform platform) {
        return getService(authServices, platform, "Authorization");
    }

    public EcommOrderService getOrderService(Platform platform) {
        return getService(orderServices, platform, "Order");
    }

    public EcommFulfillmentService getFulfillmentService(Platform platform) {
        return getService(fulfillmentServices, platform, "Fulfillment");
    }

    public EcommLogisticsService getLogisticsService(Platform platform) {
        return getService(logisticsServices, platform, "Logistics");
    }

    private <T> T getService(Map<Platform, T> serviceMap, Platform platform, String serviceName) {
        if (serviceMap == null) {
            throw new IllegalStateException("服务映射表尚未初始化。");
        }
        T service = serviceMap.get(platform);
        if (service == null) {
            throw new IllegalArgumentException(
                String.format("没有为平台 [%s] 找到对应的 [%s] 服务实现。当前已注册的服务平台有: %s",
                    platform.getDescription(), serviceName, serviceMap.keySet())
            );
        }
        return service;
    }
}
