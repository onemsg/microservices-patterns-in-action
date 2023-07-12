package com.onemsg.vertxservice;

import java.time.Duration;

import com.onemsg.vertxservice.patterns.AsyncJobWorkerVerticle;
import com.onemsg.vertxservice.patterns.RateLimitHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Request;
import lombok.extern.slf4j.Slf4j;

/**
 * MainVerticle
 */
@Slf4j
public class MainVerticle extends AbstractVerticle{

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Redis redis = Redis.createClient(vertx, new RedisOptions().setMaxPoolSize(12).setMaxPoolWaiting(48));
        RateLimitHandler rateLimitHandler = RateLimitHandler.createRedis(redis, 100, Duration.ofSeconds(60));

        HealthChecks hc = HealthChecks.create(vertx);
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(hc);

        healthCheckHandler.register("kafka", 2000, promise -> {
            vertx.eventBus().request("health-check", "kafka")
                .onComplete( ar -> {
                    if (ar.succeeded()) {
                        promise.tryComplete(Status.OK());
                    } else {
                        promise.tryFail(ar.cause());
                    }
                });
        });

        healthCheckHandler.register("redis", 2000, promise -> {
            redis.send(Request.cmd(Command.PING))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        promise.tryComplete(Status.OK());
                    } else {
                        promise.tryFail(ar.cause());
                    }
                });
        });

        vertx.setPeriodic(2000, 60_1000, id -> {
            hc.checkStatus().onSuccess( r -> {
                log.info("Heatch check - {}", r.toJson());
            } );
        });


        var f1 = vertx.deployVerticle( () -> new WebVerticle(rateLimitHandler, healthCheckHandler), 
            new DeploymentOptions().setInstances(2)
        );

        var f2 = vertx.deployVerticle(AsyncJobWorkerVerticle.class, new DeploymentOptions());

        Future.all(f1, f2)
            .onSuccess(f -> startPromise.complete())
            .onFailure(startPromise::fail);
    }
}