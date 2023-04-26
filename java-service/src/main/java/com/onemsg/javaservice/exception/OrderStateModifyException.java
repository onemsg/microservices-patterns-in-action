package com.onemsg.javaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

import lombok.Getter;

/**
 * OrderStateModifyException
 */
@Getter
public class OrderStateModifyException extends ErrorResponseException {

    private static final HttpStatus STATUS_CODE = HttpStatus.BAD_REQUEST;

    private final long orderId;

    public OrderStateModifyException(long orderId, String fromState, String toState) {
        super(STATUS_CODE);
        setTitle("Order state cannot modify");
        setDetail(String.format("The order [id=%s] state cannot from %s to %s",orderId, fromState, toState));
        getBody().setProperty("orderId", orderId);
        getBody().setProperty("fromState", fromState);
        getBody().setProperty("toState", toState);
        this.orderId = orderId;
    }
}