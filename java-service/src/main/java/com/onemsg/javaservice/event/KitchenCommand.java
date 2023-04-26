package com.onemsg.javaservice.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KitchenCommand implements Command {

    private String name;
    private long restaurantId;
    private long tickedId;
    private long orderId;
    private long createdTimestamp;

    public enum Type {
        CREATE_TICKET,
        APPROVE_TICKET,
        REJECT_TICKET;
    }

    public static KitchenCommand createTicket(long restaurantId, long orderId) {
        return new KitchenCommandBuilder()
            .name(Type.CREATE_TICKET.name())
            .restaurantId(restaurantId)
            .orderId(orderId)
            .createdTimestamp(System.currentTimeMillis())
            .build();
    }

    public static KitchenCommand approveTicket(long tickedId, long orderId) {
        return new KitchenCommandBuilder()
                .name(Type.APPROVE_TICKET.name())
                .tickedId(tickedId)
                .orderId(orderId)
                .createdTimestamp(System.currentTimeMillis())
                .build();
    }

    public static KitchenCommand rejectTicket(long tickedId, long orderId) {
        return new KitchenCommandBuilder()
                .name(Type.APPROVE_TICKET.name())
                .tickedId(tickedId)
                .orderId(orderId)
                .createdTimestamp(System.currentTimeMillis())
                .build();
    }

}
