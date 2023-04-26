package com.onemsg.javaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.Getter;

/**
 * OrderAccessDeniedException
 */
@Getter
public class OrderAccessDeniedException extends ResponseStatusException{

    private static final HttpStatus STATUS_CODE = HttpStatus.FORBIDDEN;

    private final long orderId;
    
    public OrderAccessDeniedException(long orderId) {
        super(STATUS_CODE);
        setTitle("Order access denied");
        setDetail(String.format("You don't have permission to access the order [id=%s]", orderId));
        getBody().setProperty("orderId", orderId);
        this.orderId = orderId;
    }
}