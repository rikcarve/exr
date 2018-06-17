package ch.carve.exr;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;

public class ServerVerticle extends AbstractVerticle {
	private static final String CREATE_KEYSPACE = "CREATE KEYSPACE IF NOT EXISTS exr WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}";
	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS exr.rates_query (  \r\n" + 
			"  baseCurrency int,  \r\n" + 
			"  toCurrency int,\r\n" + 
			"  effectiveFrom date, \r\n" + 
			"  rate double,  \r\n" + 
			"  PRIMARY KEY ((baseCurrency, toCurrency), effectiveFrom)  \r\n" + 
			") WITH CLUSTERING ORDER BY ( effectivefrom DESC );\r\n" + 
			"";
	private Session session;
	private ExchangerateAccessor exr;

    @Override
    public void start(Future<Void> fut) {
        // set up cassandra
        Cluster cluster = Cluster.builder().addContactPoint("localhost").build();
        session = cluster.connect();
        session.execute(CREATE_KEYSPACE);
        session.execute(CREATE_TABLE);
        MappingManager manager = new MappingManager(session);
        exr = manager.createAccessor(ExchangerateAccessor.class);
        
        Router router = Router.router(vertx);
        router.route().handler(new LoggerHandlerImpl(LoggerFormat.SHORT));
        router.get("/").handler(  this::hello);
        router.get("/exr").handler(this::exchangerate);

        // new HttpServerOptions().setLogActivity(true)
        vertx.createHttpServer().requestHandler(router::accept).listen(8080, result -> {
            if (result.succeeded()) {
                fut.complete();
            } else {
                fut.fail(result.cause());
            }
        });
    }

    private void exchangerate(RoutingContext routingContext) {
        int baseCurrency = Integer.valueOf(routingContext.request().getParam("base"));
        int toCurrency = Integer.valueOf(routingContext.request().getParam("to"));
        ListenableFuture<Exchangerate> future = exr.getOne(baseCurrency, toCurrency);
        Futures.addCallback(future, new FutureCallback<Exchangerate>() {
			@Override
			public void onSuccess(Exchangerate result) {
				routingContext.response().end(String.valueOf(result.getRate()));				
			}
			@Override
			public void onFailure(Throwable t) {
				routingContext.response().setStatusCode(500).end("Failed");
			}
		});
    }

    private void hello(RoutingContext routingContext) {
        routingContext.response().end("Hello Vert.x");
    }
}
