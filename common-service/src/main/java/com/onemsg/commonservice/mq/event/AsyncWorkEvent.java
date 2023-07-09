package com.onemsg.commonservice.mq.event;

/**
 * AsyncWork Event
 */
public record AsyncWorkEvent(
    String jobId,
    String name,
    String value) {
    
}