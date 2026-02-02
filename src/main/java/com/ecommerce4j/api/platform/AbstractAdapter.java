package com.ecommerce4j.api.platform;

import com.ecommerce4j.api.exception.EcommIntegrationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 抽象适配器基类。
 * <p>
 * 为所有具体的平台适配器提供通用的功能，包括：
 * 1. 一个预配置的 OkHttpClient 实例，用于执行 HTTP 请求。
 * 2. 一个预配置的 ObjectMapper 实例，用于 JSON 序列化和反序列化。
 * 3. 封装了执行请求和处理响应（包括错误处理）的通用方法。
 */
public abstract class AbstractAdapter {

    /**
     * 可复用的 OkHttp 客户端，用于发送 HTTP 请求。
     * 配置了合理的连接、读取和写入超时时间。
     */
    protected final OkHttpClient httpClient;

    /**
     * 用于通用文件下载的 OkHttp 客户端。
     * 这是一个“干净”的实例，不包含任何业务相关的拦截器。
     */
    protected final OkHttpClient downloadClient;

    /**
     * 可复用的 Jackson ObjectMapper，用于处理 JSON 数据。
     * - 注册了 JavaTimeModule 以支持 Java 8 的日期和时间类型（如 Instant）。
     * - 配置为在反序列化时忽略未知的 JSON 属性，以增强向前兼容性。
     */
    protected final ObjectMapper objectMapper;

    /**
     * 构造函数，在子类实例化时初始化 httpClient 和 objectMapper。
     */
    public AbstractAdapter() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(new ApiLoggingInterceptor())
            .build();

        this.downloadClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // 下载文件可能需要更长的读取超时
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 执行一个 HTTP 请求，并将 JSON 响应体反序列化为指定的 Java 类型。
     *
     * @param request      构建好的 OkHttp Request 对象。
     * @param responseType 期望的响应体 Java 类型的 Class 对象。
     * @param <T>          泛型，表示期望的响应类型。
     * @return 反序列化后的 Java 对象。
     * @throws EcommIntegrationException 如果请求失败（HTTP 状态码非 2xx）或在处理过程中发生 I/O 错误。
     */
    protected <T> T executeRequest(Request request, Class<T> responseType) {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String bodyString = (body != null) ? body.string() : null;

            if (!response.isSuccessful()) {
                String errorMessage = String.format("API请求至 %s 失败，状态码 %d: %s", request.url(), response.code(), bodyString);
                throw new EcommIntegrationException(errorMessage);
            }

            if (bodyString == null || bodyString.isEmpty()) {
                throw new EcommIntegrationException("API响应体为空，但期望返回JSON。");
            }

            return objectMapper.readValue(bodyString, responseType);

        } catch (IOException e) {
            throw new EcommIntegrationException("执行HTTP请求或解析响应失败：" + request.url(), e);
        }
    }

    /**
     * 执行一个 HTTP 请求，并将 JSON 响应体反序列化为指定的复杂泛型类型（如 List<T> 或 Map<K, V>）。
     *
     * @param request       构建好的 OkHttp Request 对象。
     * @param typeReference 用于捕获复杂泛型类型的 Jackson TypeReference 对象。例如 new TypeReference<List<MyObject>>() {}。
     * @param <T>           泛型，表示期望的响应类型。
     * @return 反序列化后的 Java 对象。
     * @throws EcommIntegrationException 如果请求失败或在处理过程中发生错误。
     */
    protected <T> T executeRequest(Request request, TypeReference<T> typeReference) {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String bodyString = (body != null) ? body.string() : null;

            if (!response.isSuccessful()) {
                String errorMessage = String.format("API请求至 %s 失败，状态码 %d: %s", request.url(), response.code(), bodyString);
                throw new EcommIntegrationException(errorMessage);
            }
            if (bodyString == null || bodyString.isEmpty()) {
                throw new EcommIntegrationException("API响应体为空，但期望返回JSON。");
            }

            return objectMapper.readValue(bodyString, typeReference);

        } catch (IOException e) {
            throw new EcommIntegrationException("执行HTTP请求或解析响应失败：" + request.url(), e);
        }
    }

    /**
     * 执行一个 HTTP 请求，并直接返回响应体的二进制字节数组。
     * 主要用于下载文件，如 PDF 或 ZPL 格式的运单。
     *
     * @param request 构建好的 OkHttp Request 对象。
     * @return 包含文件内容的字节数组。
     * @throws EcommIntegrationException 如果请求失败或在下载过程中发生 I/O 错误。
     */
    protected byte[] executeRequestForBytes(Request request) {
        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 读取错误体以提供更多信息
                ResponseBody errorBody = response.body();
                String errorBodyString = (errorBody != null) ? errorBody.string() : "[无错误体]";
                String errorMessage = String.format("API文件下载请求至 %s 失败，状态码 %d: %s", request.url(), response.code(), errorBodyString);
                throw new EcommIntegrationException(errorMessage);
            }
            return Objects.requireNonNull(response.body(), "成功的文件下载请求响应体为 null。").bytes();
        } catch (IOException e) {
            throw new EcommIntegrationException("文件下载失败：" + request.url(), e);
        }
    }
}
