package com.onemsg.vertxservice.kitchen;

import com.onemsg.vertxservice.web.StatusResponseException;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class KitchenController {
    
    public void mount(Router router) {
        
        router.get("/api/exception").handler(this::handleException);
    }


    private void handleException(RoutingContext context) {
        throw StatusResponseException.create(400, "请求参数错误");
    }

    
}
