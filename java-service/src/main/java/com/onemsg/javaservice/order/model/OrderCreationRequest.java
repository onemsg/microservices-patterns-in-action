package com.onemsg.javaservice.order.model;

import java.util.List;

import lombok.Data;

@Data
public class OrderCreationRequest {
    
    private long consumerId;
    private List<Long> foods;
    private long restaurantId;
}
