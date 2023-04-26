package com.onemsg.javaservice.order;

import com.onemsg.javaservice.order.model.Order;

import lombok.Data;

@Data
public class CreateOrderSagaState {
    
    private long orderId;
    private Order order;
    private long ticketId;

}
