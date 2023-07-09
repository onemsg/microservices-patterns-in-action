package com.onemsg.commonservice.store;

import java.util.Objects;

import io.jsondb.JsonDBTemplate;

/**
 * AsyncJobStore.
 * 
 * 基于 <a href="https://jsondb.io/">Jsondb</a> 的存储
 */
public class AsyncJobStore {
    
    public static final String DBFILES_LOCATION = Objects.requireNonNullElse(System.getenv("JSONDBFILES_LOCATION"), "c:\\jsondb");
    
    private static final JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(DBFILES_LOCATION, "com.onemsg.commonservice.store");

    static {
        if (!jsonDBTemplate.collectionExists(AsyncJob.class)) {
            jsonDBTemplate.createCollection(AsyncJob.class);
        }
        if (!jsonDBTemplate.collectionExists(AsyncJobResult.class)) {
            jsonDBTemplate.createCollection(AsyncJobResult.class);
        }  
    }

    public static JsonDBTemplate jsondb() {
        return jsonDBTemplate;
    }
}
