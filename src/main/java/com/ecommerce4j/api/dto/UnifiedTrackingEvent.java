package com.ecommerce4j.api.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class UnifiedTrackingEvent {
    /**
     * 物流事件描述
     */
    private String description;
    /**
     * 物流事件发生时间
     */
    private Instant time;
    /**
     * 物流事件发生地点
     */
    private String location;
}
