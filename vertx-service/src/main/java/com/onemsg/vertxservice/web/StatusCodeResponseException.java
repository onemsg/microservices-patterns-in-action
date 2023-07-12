package com.onemsg.vertxservice.web;

public class StatusCodeResponseException extends RuntimeException{
    
    private final int status;

    public StatusCodeResponseException(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static StatusCodeResponseException create(int status) {
        return new StatusCodeResponseException(status);
    }
}
