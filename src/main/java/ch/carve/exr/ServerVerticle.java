package ch.carve.exr;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;

public class ServerVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {

        Router router = Router.router(vertx);
        router.route().handler(new LoggerHandlerImpl(LoggerFormat.SHORT));
        router.get("/").handler(this::hello);
        router.get("/exr").handler(this::handle);

        // new HttpServerOptions().setLogActivity(true)
        vertx.createHttpServer().requestHandler(router::accept).listen(8080, result -> {
            if (result.succeeded()) {
                fut.complete();
            } else {
                fut.fail(result.cause());
            }
        });
    }

    public void handle(RoutingContext event) {
        String msg = new JsonObject()
                .put("base", event.request().getParam("base"))
                .put("to", event.request().getParam("to"))
                .encode();
        vertx.eventBus().send(ExrEvents.SINGLE_RATE.name(), msg, res -> handleResponse(res, event));
    }

    private void handleResponse(AsyncResult<Message<Object>> res, RoutingContext context) {
        if (res.succeeded()) {
            context.response().end((String) res.result().body());
        } else {
            context.response().setStatusCode(500).end();
        }
    }

    private void hello(RoutingContext routingContext) {
        routingContext.response().end("Hello Vert.x");
    }
}
