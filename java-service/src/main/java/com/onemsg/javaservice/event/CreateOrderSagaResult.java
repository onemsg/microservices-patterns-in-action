package com.onemsg.javaservice.event;

import lombok.Data;

@Data
public class CreateOrderSagaResult implements Message{
    
    private String name;
    private long orderId;
    private long consumerId;
    private long ticketId;
    private long createdTimestamp;

    public enum Type {
        CONSUMER_VERIFIED,
        CONSUMER_VERIFIED_FAILED,
        TICKET_CREATED,
        TICKET_CREATED_FAILED,
        CARD_AUTHORIZED,
        CARD_AUTHORIZED_FAILED;

        public boolean match(String name) {
            return name().equalsIgnoreCase(name);
        }
    }
}
