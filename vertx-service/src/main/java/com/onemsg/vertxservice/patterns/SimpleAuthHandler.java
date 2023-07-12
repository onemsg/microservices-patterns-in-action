package com.onemsg.vertxservice.patterns;

import java.util.Set;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class SimpleAuthHandler implements AuthenticationHandler {

    private static final Set<String> ALLOW_USERIDS = Set.of("spring", "vertx", "helidon");

    @Override
    public void handle(RoutingContext context) {
        String userId = context.request().getHeader("X-Auth-UserId");
        // 认证和鉴权用户
        if (userId == null) {
            context.response().setStatusCode(401).end();
        } else if (!verifyUserId(userId)) {
            context.response().setStatusCode(403).end();
        } else {
            context.setUser(User.create(new JsonObject().put("userId", userId)));
            context.next();
        }
    }
    
    public static SimpleAuthHandler create() {
        return new SimpleAuthHandler();
    }

    private boolean verifyUserId(String userId) {
        return ALLOW_USERIDS.contains(userId) || userId.startsWith("LoadTestUser-");
    }
}
