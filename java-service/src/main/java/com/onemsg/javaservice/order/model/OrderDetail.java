package com.onemsg.javaservice.order.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDetail {
    private long id;
    private long consumerId;
    private List<Long> foods;
    private long totalMoney;
    private long restaurantId;

    public static OrderDetail build(Order order) {
        return new OrderDetailBuilder()
            .id(order.getId())
            .consumerId(order.getConsumerId())
            .foods(order.getFoods())
            .totalMoney(order.getTotalMoney())
            .restaurantId(order.getRestaurantId())
            .build();
    }
}
