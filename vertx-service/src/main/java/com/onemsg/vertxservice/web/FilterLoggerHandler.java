package com.onemsg.vertxservice.web;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormatter;
import io.vertx.ext.web.handler.LoggerHandler;

public class FilterLoggerHandler implements LoggerHandler {

    private final LoggerHandler handler;

    private final Predicate<RoutingContext> filter;

    public FilterLoggerHandler(LoggerHandler loggerHandler, Predicate<RoutingContext> filter) {
        this.handler = Objects.requireNonNull(loggerHandler);
        this.filter = Objects.requireNonNullElse(filter, context -> true);
    }

    @Override
    public void handle(RoutingContext context) {
        if (filter.test(context)) {
            handler.handle(context);
        } else {
            context.next();
        }
    }

    public static LoggerHandler create(LoggerHandler loggerHandler) {
        return create(loggerHandler, null);
    }

    public static LoggerHandler create(LoggerHandler loggerHandler, Predicate<RoutingContext> filter) {
        return new FilterLoggerHandler(loggerHandler, filter);
    }

    @Override
    public LoggerHandler customFormatter(Function<HttpServerRequest, String> formatter) {
        return handler.customFormatter(formatter);
    }

    @Override
    public LoggerHandler customFormatter(LoggerFormatter formatter) {
        return handler.customFormatter(formatter);
    }
}
