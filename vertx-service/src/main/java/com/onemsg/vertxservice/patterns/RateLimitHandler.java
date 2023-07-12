package com.onemsg.vertxservice.patterns;

import java.time.Duration;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.client.Redis;

/**
 * RateLimitHandler
 * 
 * <p>
 * 实现 <a href=
 * "https://learn.microsoft.com/zh-cn/azure/architecture/patterns/rate-limiting-pattern">
 * 速率限制模式 </a>
 */
public interface RateLimitHandler extends Handler<RoutingContext> {
    
    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    /** 下次重置时间，单位 s */
    public static final String HEADER_RESET = "X-RateLimit-Reset";  

    public static final int STATUS_CODE_LIMITED = 429;
    
    @Override
    public void handle(RoutingContext context);

    public static RateLimitHandler createLocal(int limit, Duration duration) {
        return new LocalRateLimitHandler(limit, duration);
    }

    public static RateLimitHandler createRedis(Redis redis, int limit, Duration duration) {
        return new RedisRateLimitHandler(redis, limit, duration);
    }

    public record LimitDetail(int limit, int count, long reset) {

        public boolean canAccess() {
            return count <= limit;
        }

        public int remaining() {
            return count <= limit ? limit - count : 0;
        }
    }

    /**
     * Add X-RateLimit-* headers
     * @param context
     * @param limitDetail
     */
    public static void addHeadersEndHandler(RoutingContext context, LimitDetail limitDetail) {
        context.addHeadersEndHandler( v -> {
            var detail = limitDetail;
            context.response()
                    .putHeader(HEADER_LIMIT, String.valueOf(detail.limit()))
                    .putHeader(HEADER_REMAINING, String.valueOf(detail.remaining()))
                    .putHeader(HEADER_RESET, String.valueOf(detail.reset()));
        });
    }

    /**
     * End response with status code 429
     * @param context
     */
    public static void endLimited(RoutingContext context) {
        context.response().setStatusCode(STATUS_CODE_LIMITED).end();
    }
}