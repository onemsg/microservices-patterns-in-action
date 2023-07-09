package com.onemsg.commonservice.store;

import java.time.LocalDateTime;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import lombok.Data;

@Data
@Document(collection = "asyncJobs", schemaVersion= "1.0")
public class AsyncJob {
    
    @Id
    private String id;

    private String name;

    private String value;

    private State state;

    private int executionTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    public enum State {
        NEW,
        RUNNING,
        PAUSE,
        FINISH,
        CANNEL,
        FAILURE
    }
}
