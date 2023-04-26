package com.onemsg.javaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

import lombok.Getter;

@Getter
public class OrderNotExistedException extends ErrorResponseException{
    
    private static final HttpStatus STATUS_CODE = HttpStatus.NOT_FOUND;
    
    private final long orderId;

    public OrderNotExistedException(long orderId) {
        super(STATUS_CODE);
        setTitle("Order not existed");
        setDetail(String.format("The order [id=%s] not existed", orderId));
        getBody().setProperty("orderId", orderId);
        this.orderId = orderId;
    }
}
