package com.onemsg.vertxservice.web;

import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class CollectEventHandler implements Handler<RoutingContext>{

    public static final String PATH = "/collect-event";

    @Override
    public void handle(RoutingContext event) {
        event.request().headers()
            .forEach((k,v) -> {
                System.out.println(k + ": " + v);
            });
        System.out.println();

        event.json(Map.of("status", "OK"));
    }

    public void mount(Router router){
        router.post(PATH).handler(this);
    }
    
    public static CollectEventHandler create() {
        return new CollectEventHandler();
    }
}
