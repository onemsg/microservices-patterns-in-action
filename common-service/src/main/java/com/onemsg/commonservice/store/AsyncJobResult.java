package com.onemsg.commonservice.store;

import java.time.LocalDateTime;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import lombok.Data;

@Data
@Document(collection = "asyncJobResults", schemaVersion= "1.0")
public class AsyncJobResult {
    
    @Id
    private String jobId;

    private String result;

    private LocalDateTime finishedTime;
}
