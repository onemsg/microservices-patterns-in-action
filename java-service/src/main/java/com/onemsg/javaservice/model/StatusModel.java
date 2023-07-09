package com.onemsg.javaservice.model;

public record StatusModel(
    int status,
    String message,
    Object data
) {
    
    
}
