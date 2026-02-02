package com.ecommerce4j.api.platform;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一个自定义的OkHttp拦截器，用于记录详细的API请求和响应日志。
 * 采用重新构建响应体的方式，以确保日志记录后，响应流依然可用。
 */
@Slf4j
public class ApiLoggingInterceptor implements Interceptor {

    private static final int MAX_BODY_LENGTH_TO_LOG = 1500; // 响应体预览长度
    // 只保留这些我们关心的响应头
    private static final List<String> RETAINED_RESPONSE_HEADERS = Arrays.asList(
        "content-type", "date", "x-tts-logid", "x-tt-logid"
    );

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        // 格式化并记录请求日志
        String requestBody = getRequestBody(request);
        log.info("【Ecommerce4j-API请求】=> {} {} | 请求体: {}", request.method(), request.url(), requestBody);

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            log.error("【Ecommerce4j-API请求异常】=> {} {}: {}", request.method(), request.url(), e.getMessage());
            throw e;
        }

        // --- 核心修改点：记录响应并重新构建它 ---
        ResponseBody responseBody = response.body();
        String responseBodyString = "";
        if (responseBody != null) {
            responseBodyString = responseBody.string();
        }

        // 格式化并记录响应日志
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        String truncatedBody = truncateBody(responseBodyString);
        String filteredHeaders = filterAndFormatHeaders(response.headers());

        log.info("【Ecommerce4j-API响应】<= {} {} | 耗时: {}ms | 响应体: {} | 响应头: {}",
            response.code(),
            response.message(),
            durationMs,
            truncatedBody,
            filteredHeaders);

        // 使用相同的字符串内容创建一个新的响应体
        ResponseBody newResponseBody = ResponseBody.create(responseBodyString, responseBody != null ? responseBody.contentType() : null);

        // 构建一个新的 Response 对象
        return response.newBuilder().body(newResponseBody).build();
    }

    private String getRequestBody(Request request) {
        if (request.body() == null) {
            return "[无]";
        }
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[读取请求体失败]";
        }
    }

    private String truncateBody(String body) {
        if (body == null || body.isEmpty()) {
            return "[空]";
        }
        if (body.length() > MAX_BODY_LENGTH_TO_LOG) {
            return body.substring(0, MAX_BODY_LENGTH_TO_LOG) + "...";
        }
        return body;
    }

    private String filterAndFormatHeaders(okhttp3.Headers headers) {
        if (headers == null || headers.size() == 0) {
            return "[无]";
        }
        return headers.toMultimap().entrySet().stream()
            .filter(entry -> RETAINED_RESPONSE_HEADERS.contains(entry.getKey().toLowerCase()))
            .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
            .collect(Collectors.joining(" | "));
    }
}
