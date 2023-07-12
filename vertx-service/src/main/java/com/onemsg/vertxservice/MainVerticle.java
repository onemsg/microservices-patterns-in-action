package com.onemsg.vertxservice;

import java.time.Duration;
import java.util.Map;

import com.onemsg.vertxservice.kitchen.KitchenController;
import com.onemsg.vertxservice.patterns.AsyncJobWorkerVerticle;
import com.onemsg.vertxservice.patterns.RateLimitHandler;
import com.onemsg.vertxservice.patterns.SimpleAuthHandler;
import com.onemsg.vertxservice.web.ExceptionHandler;
import com.onemsg.vertxservice.web.FilterLoggerHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.extern.slf4j.Slf4j;

/**
 * MainVerticle
 */
@Slf4j
public class MainVerticle extends AbstractVerticle{

    private static final int PORT = Integer.parseInt(System.getenv("VERTX_SERVICE_PORT"));

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Router router = Router.router(vertx);

        LoggerHandler loggerHandler = FilterLoggerHandler.create(LoggerHandler.create(LoggerFormat.TINY),
            context -> context.request().getHeader("X-LoadTest") == null);

        router.route().handler(loggerHandler);
        router.route().handler(TimeoutHandler.create(5000));
        router.route().handler(BodyHandler.create());

        router.route("/api/*").handler(SimpleAuthHandler.create());

        Redis redis = Redis.createClient(vertx);
        RateLimitHandler rateLimitHandler = RateLimitHandler.createRedis(redis, 100, Duration.ofSeconds(60));
        router.route("/api/*").handler(rateLimitHandler);
        
        router.route("/api/*").failureHandler(ExceptionHandler.create());

        router.get("/api/test-data").handler(context -> {
            context.json(Map.of("data", "test-data"));
        });

        router.route().failureHandler(ErrorHandler.create(vertx));

        HealthChecks hc = HealthChecks.create(vertx);
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.createWithHealthChecks(hc);
        router.route("/health").handler(healthCheckHandler);

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

        KitchenController kitchenController = new KitchenController();
        kitchenController.mount(router);

        var f1 = vertx.createHttpServer(new HttpServerOptions())
            .requestHandler(router)
            .listen(PORT)
            .onSuccess(http -> {
                log.info("Server running on http://localhost:{}", http.actualPort());
            });
            
        var f2 = vertx.deployVerticle(AsyncJobWorkerVerticle.class, new DeploymentOptions());

        Future.all(f1, f2)
            .onSuccess(f -> startPromise.complete())
            .onFailure(startPromise::fail);

    }
}