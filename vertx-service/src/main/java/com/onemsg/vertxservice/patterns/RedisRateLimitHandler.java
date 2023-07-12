package com.onemsg.vertxservice.patterns;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.onemsg.vertxservice.web.StatusCodeResponseException;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisRateLimitHandler implements RateLimitHandler{

    private static final String SCRIPT_CLASSPATH = "/script/rateLimiter.lua";

    private final Redis redis;
    private final int limit;
    private final long duration;


    public RedisRateLimitHandler(Redis redis, int limit, Duration duration) {
        this.redis = Objects.requireNonNull(redis);
        this.limit = limit;
        this.duration = Objects.requireNonNull(duration).toSeconds();
    }

    private final AtomicReference<String> scriptSHA = new AtomicReference<>("null");

    @Override
    public void handle(RoutingContext context) {
        String userId = context.user().get("userId");

        tryAcquire(userId)
            .onSuccess(limitDetail -> {
                RateLimitHandler.addHeadersEndHandler(context, limitDetail);
                if (limitDetail.canAccess()) {
                    context.next();
                } else {
                    RateLimitHandler.endLimited(context);
                }
            }).onFailure(context::fail);
    }

    private Future<LimitDetail> tryAcquire(String userId) {
        String key = "rl:userid:" + userId;
        return sendEval(key, limit, duration)
            .compose(Future::succeededFuture, t -> {
                if ( t.getMessage().contains("NOSCRIPT") ) {
                    return loadScript().compose(res -> sendEval(key, limit, duration));
                } else {
                    return Future.failedFuture(t);
                }
            })
            .map( res -> {
                int count = res.get(0).toInteger();
                long reset = res.get(1).toLong();
                return new LimitDetail(limit, count, reset);
            });
    }
    
    private Future<Response> sendEval(String key, int limit, long duration) {
        return redis.send(Request.cmd(Command.EVALSHA, scriptSHA.toString(), 1, key, limit, duration));
    }

    private Future<Response> loadScript() {
        String script = null;
        try {
            var bytes = RedisRateLimitHandler.class.getResourceAsStream(SCRIPT_CLASSPATH).readAllBytes();
            script = new String(bytes);
        } catch (Exception e) {
            log.error("Read script failed - {} {}", SCRIPT_CLASSPATH, e.getMessage());
            return Future.failedFuture(StatusCodeResponseException.create(500));
        }
        return redis.send(Request.cmd(Command.SCRIPT, "load", script))
            .onSuccess(res -> {
                scriptSHA.set(res.toString());
                log.info("Redis script load success - {}", res);
            }).onFailure(t -> {
                log.error("Redis script load failed - {}", t);
            });
    }

}
