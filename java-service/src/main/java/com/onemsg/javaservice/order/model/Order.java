package com.onemsg.javaservice.order.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class Order {
    
    private long id;
    private State state;
    private long consumerId;
    private List<Long> foods;
    private int totalMoney;
    private long restaurantId;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdatedTime;

    public enum State {
        APPROVAL_PENDING,
        APPROVED,
        FINISH,
        CANCEL,
        REJECT;
    }

    public List<Long> getFoods() {
        return foods != null ? foods : List.of();
    }

    public boolean canApproved() {
        return state == State.APPROVAL_PENDING || state == State.APPROVED;
    }

    public boolean canCancel() {
        return state != State.FINISH && state != State.REJECT;
    }

    public boolean canFinish() {
        return state != State.CANCEL && state != State.REJECT;
    }

    public boolean canReject() {
        return state != State.FINISH || state != State.CANCEL;
    }
}
