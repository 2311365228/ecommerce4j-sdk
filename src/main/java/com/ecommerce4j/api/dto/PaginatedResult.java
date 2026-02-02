package com.ecommerce4j.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果封装类
 * @param <T> 数据实体的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResult<T> {

    /**
     * 当前页的数据列表
     */
    private List<T> data;

    /**
     * 用于获取下一页数据的令牌 (或游标)
     * 如果为null或空，表示没有下一页
     */
    private String nextPageToken;
}
