package ch.carve.exr;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExchangerateVerticle extends AbstractVerticle {
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
    public void start() throws Exception {
        MessageConsumer<String> consumer = vertx.eventBus().consumer(ExrEvents.SINGLE_RATE.name());
        consumer.handler(this::handler);
        Cluster cluster = Cluster.builder().addContactPoint("localhost").build();
        session = cluster.connect();
        session.execute(CREATE_KEYSPACE);
        session.execute(CREATE_TABLE);
        MappingManager manager = new MappingManager(session);
        exr = manager.createAccessor(ExchangerateAccessor.class);
    }

    public void handler(Message<String> message) {
        JsonObject data = new JsonObject(message.body());
        int baseCurrency = Integer.valueOf(data.getString("base"));
        int toCurrency = Integer.valueOf(data.getString("to"));

        log.info("Query for base: {} to: {}", baseCurrency, toCurrency);
        ListenableFuture<Exchangerate> future = exr.getOne(baseCurrency, toCurrency,
                LocalDate.fromMillisSinceEpoch(System.currentTimeMillis()));
        Futures.addCallback(future, new FutureCallback<Exchangerate>() {
            @Override
            public void onSuccess(Exchangerate result) {
                message.reply(Json.encode(String.valueOf(result.getRate())));
            }

            @Override
            public void onFailure(Throwable t) {
                message.fail(500, message.body());
            }
        });
    }

}
