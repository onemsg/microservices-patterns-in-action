package com.onemsg.authservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * MainVerticle
 */
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        
        startPromise.complete();
    }
}