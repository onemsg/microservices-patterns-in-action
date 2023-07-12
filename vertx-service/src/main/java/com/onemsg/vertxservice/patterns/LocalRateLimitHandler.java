package com.onemsg.vertxservice.patterns;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地速率限制处理器实现
 */
@Slf4j
public class LocalRateLimitHandler implements RateLimitHandler {

    private static class RateLimiter {
        
        private final int limit;
        private final AtomicInteger count;
        private final int duration; // 单位 s
        private volatile long reset; // 单位 s

        private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        private final Lock rl = lock.readLock();
        private final Lock wl = lock.writeLock();

        public RateLimiter(int limit, Duration duration) {
            this.limit = limit;
            this.duration = (int) duration.toSeconds();
            count = new AtomicInteger(0);
        }

        public LimitDetail tryAcquire() {
            rl.lock();
            if (needReset()) {
                rl.unlock();
                wl.lock();
                try {
                    if (needReset()) {
                        count.set(0);
                        reset = Instant.now().plusSeconds(duration).getEpochSecond();
                    }
                    rl.lock();
                } finally {
                    wl.unlock();
                }
            }
            try{
                return new LimitDetail(limit, count.incrementAndGet(), reset);
            } finally {
                rl.unlock();
            }
        }

        public int getLimit() {
            return limit;
        }

        private boolean needReset() {
            return reset <= Instant.now().getEpochSecond();
        }

    }

    private final Cache<String, RateLimiter> rateLimiters = Caffeine.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS)
        .maximumSize(100_000)
        .build();

    private final int limit;

    private final Duration duration;

    public LocalRateLimitHandler(int limit, Duration duration) {
        this.limit = limit;
        this.duration = duration;
    }

    @Override
    public void handle(RoutingContext context) {

        String userId = context.user().get("userId");
        RateLimiter rateLimiter = rateLimiters.get(userId, k -> newRateLimiter());
        var limitDetail = rateLimiter.tryAcquire();

        RateLimitHandler.addHeadersEndHandler(context, limitDetail);

        if (limitDetail.canAccess()) {
            context.next();
        } else {
            RateLimitHandler.endLimited(context);
        }
    }

    private RateLimiter newRateLimiter() {
        return new RateLimiter(limit, duration);
    }

}
