package com.onemsg.javaservice.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCommand implements Command {
    
    private String name;
    private long orderId;
    private long createdTimestamp;

    public enum Type {
        APPROVE_ORDER,
        REJECT_ORDER;
    }

    public static OrderCommand approveOrder(long orderId) {
        return new OrderCommandBuilder()
            .name(Type.APPROVE_ORDER.name())
            .orderId(orderId)
            .createdTimestamp(System.currentTimeMillis())
            .build();
    }

    public static OrderCommand rejectOrder(long orderId) {
        return new OrderCommandBuilder()
                .name(Type.REJECT_ORDER.name())
                .orderId(orderId)
                .createdTimestamp(System.currentTimeMillis())
                .build();
    }


}
