package com.onemsg.vertxservice;

import java.util.concurrent.atomic.AtomicInteger;

import com.onemsg.vertxservice.kitchen.KitchenController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
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

        router.route().handler(LoggerHandler.create(LoggerFormat.TINY));
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(ErrorHandler.create(vertx));
        router.route().blockingHandler(null, false);

        KitchenController kitchenController = new KitchenController();
        kitchenController.mount(router);
        
        vertx.createHttpServer(new HttpServerOptions())
            .requestHandler(router)
            .listen(PORT)
            .onSuccess(http -> {
                log.info("Server running on http://localhost:{}", http.actualPort());
            });
    }
    
}