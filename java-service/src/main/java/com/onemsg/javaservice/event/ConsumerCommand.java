package com.onemsg.javaservice.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsumerCommand implements Command {

    private String name;
    private long consumerId;
    private long orderId;
    private long createdTimestamp;
    
    public enum Type {
        VERIFY_CONSUMER;
    }

    public static ConsumerCommand verifyConsumer(long consumerId, long orderId) {
        return new ConsumerCommandBuilder()
            .name(Type.VERIFY_CONSUMER.name())
            .consumerId(consumerId)
            .orderId(orderId)
            .createdTimestamp(System.currentTimeMillis())
            .build();
    }

    
}
