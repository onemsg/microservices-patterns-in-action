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
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    var limitDetail = ar.result();
                    RateLimitHandler.addHeadersEndHandler(context, limitDetail);
                    if (limitDetail.canAccess()) {
                        context.next();
                    } else {
                        RateLimitHandler.endLimited(context);
                    }
                } else {
                    context.fail(ar.cause());
                }
            });

    }

    private Future<LimitDetail> tryAcquire(String userId) {
        String key = "rl:userid:" + userId;
        return sendEval(key, limit, duration)
            .compose(Future::succeededFuture, t -> loadScript().compose(res -> sendEval(key, limit, duration)))
            .map( res -> {
                int count = res.get(0).toInteger();
                long reset = res.get(1).toLong();
                return new LimitDetail(limit, count, reset);
            });
    }

    private Future<Response> sendEval(String key, int limit, long duration) {
        return redis.send(Request.cmd(Command.EVALSHA, scriptSHA.toString(), 1, key, limit, duration));
    }

    private Future<Response> loadScript() throws StatusCodeResponseException {
        String script = null;
        try {
            var bytes = RedisRateLimitHandler.class.getResourceAsStream(SCRIPT_CLASSPATH).readAllBytes();
            script = new String(bytes);
        } catch (Exception e) {
            log.error("Read script failed - {} {}", SCRIPT_CLASSPATH, e.getMessage());
            return Future.failedFuture(StatusCodeResponseException.create(500));
        }
        return redis.send(Request.cmd(Command.SCRIPT, "load", script))
            .onSuccess(res -> scriptSHA.set(res.toString()));
    }

}
