package com.ecommerce4j.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Ecommerce4j 模块的自动配置类。
 * <p>
 * 这个类的作用不是手动注册Bean，而是作为一个入口，告诉Spring Boot的应用上下文：
 * "请对 'com.ecommerce4j' 这个包进行组件扫描"。
 * <p>
 * 这样，所有在 com.ecommerce4j 包及其子包中标有 @Service, @Component 等注解的类，
 * 都会被自动发现并注册为Bean，无需在这里手动列出每一个。
 * 这使得模块具有极高的可扩展性，新增适配器时，只需创建新的 @Service 类即可。
 */
@AutoConfiguration
@ComponentScan("com.ecommerce4j") // 在这里启动对本模块内部的组件扫描
public class Ecommerce4jAutoConfiguration {
}
