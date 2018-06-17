package ch.carve.exr;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;

public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {

        ExchangerateHandler exrHandler = new ExchangerateHandler();
        Router router = Router.router(vertx);
        router.route().handler(new LoggerHandlerImpl(LoggerFormat.SHORT));
        router.get("/").handler(this::hello);
        router.get("/exr").handler(exrHandler::handle);

        // new HttpServerOptions().setLogActivity(true)
        vertx.createHttpServer().requestHandler(router::accept).listen(8080, result -> {
            if (result.succeeded()) {
                fut.complete();
            } else {
                fut.fail(result.cause());
            }
        });
    }

    private void hello(RoutingContext routingContext) {
        routingContext.response().end("Hello Vert.x");
    }
}
