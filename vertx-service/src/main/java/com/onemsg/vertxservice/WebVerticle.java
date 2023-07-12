package com.onemsg.vertxservice;

import java.util.Map;

import com.onemsg.vertxservice.kitchen.KitchenController;
import com.onemsg.vertxservice.patterns.RateLimitHandler;
import com.onemsg.vertxservice.patterns.SimpleAuthHandler;
import com.onemsg.vertxservice.web.ExceptionHandler;
import com.onemsg.vertxservice.web.FilterLoggerHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class WebVerticle extends AbstractVerticle{
    
    private static final int PORT = Integer.parseInt(System.getenv("VERTX_SERVICE_PORT"));


    private final RateLimitHandler rateLimitHandler;

    private final HealthCheckHandler healthCheckHandler;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Router router = Router.router(vertx);

        LoggerHandler loggerHandler = FilterLoggerHandler.create(LoggerHandler.create(LoggerFormat.TINY),
            context -> context.request().getHeader("X-LoadTest") == null);

        router.route().handler(loggerHandler);
        router.route().handler(TimeoutHandler.create(5000));
        router.route().handler(BodyHandler.create());

        router.route("/api/*").handler(SimpleAuthHandler.create());
        router.route("/api/*").handler(rateLimitHandler);
        router.route("/api/*").failureHandler(ExceptionHandler.create());

        router.get("/api/test-data").handler(context -> {
            context.json(Map.of("data", "test-data"));
        });
        router.route().failureHandler(ErrorHandler.create(vertx));

        router.route("/health").handler(healthCheckHandler);

        KitchenController kitchenController = new KitchenController();
        kitchenController.mount(router);

        vertx.createHttpServer(new HttpServerOptions())
            .requestHandler(router)
            .listen(PORT)
            .onSuccess(http -> {
                log.info("Server running on http://localhost:{}", http.actualPort());
                startPromise.complete();
            }).onFailure(startPromise::tryFail);
    }

}
