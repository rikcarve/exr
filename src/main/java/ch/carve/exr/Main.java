package ch.carve.exr;

import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ServerVerticle());
        vertx.deployVerticle(new ExchangerateVerticle());
    }
}
