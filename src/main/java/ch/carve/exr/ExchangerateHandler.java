package ch.carve.exr;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ExchangerateHandler implements Handler<RoutingContext> {
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

    public ExchangerateHandler() {
        // set up cassandra
        Cluster cluster = Cluster.builder().addContactPoint("localhost").build();
        session = cluster.connect();
        session.execute(CREATE_KEYSPACE);
        session.execute(CREATE_TABLE);
        MappingManager manager = new MappingManager(session);
        exr = manager.createAccessor(ExchangerateAccessor.class);
    }

    @Override
    public void handle(RoutingContext event) {
        int baseCurrency = Integer.valueOf(event.request().getParam("base"));
        int toCurrency = Integer.valueOf(event.request().getParam("to"));

        ListenableFuture<Exchangerate> future = exr.getOne(baseCurrency, toCurrency,
                LocalDate.fromMillisSinceEpoch(System.currentTimeMillis()));
        Futures.addCallback(future, new FutureCallback<Exchangerate>() {
            @Override
            public void onSuccess(Exchangerate result) {
                event.response().end(String.valueOf(result.getRate()));
            }

            @Override
            public void onFailure(Throwable t) {
                event.response().setStatusCode(500).end("Failed");
            }
        });

    }

}
