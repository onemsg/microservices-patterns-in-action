package com.onemsg.javaservice.event;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class AccountingCommand implements Command {
    
    private String name;
    private long consumerId;
    private int money;
    private long orderId;
    private long createdTimestamp;

    public enum Type {
        AUTHORIZED_CARD;
    }

    public static AccountingCommand authorizedCard(long consumerId, int money, long orderId) {
        return new AccountingCommandBuilder()
            .name(Type.AUTHORIZED_CARD.name())
            .consumerId(consumerId)
            .money(money)
            .orderId(orderId)
            .createdTimestamp(System.currentTimeMillis())
            .build();
    }

}
