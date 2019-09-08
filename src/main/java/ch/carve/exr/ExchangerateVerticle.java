package ch.carve.exr;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class ExchangerateVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
    private Mapper<Exchangerate> mapper;

    @Override
    public void start() throws Exception {
        MessageConsumer<String> consumer = vertx.eventBus().consumer(ExrEvents.SINGLE_RATE.name());
        consumer.handler(this::handler);
        MessageConsumer<String> consumer2 = vertx.eventBus().consumer(ExrEvents.CREATE.name());
        consumer2.handler(this::handleCreate);
        Cluster cluster = Cluster.builder().addContactPoint("192.168.91.80").build();
        session = cluster.connect();
        session.execute(CREATE_KEYSPACE);
        session.execute(CREATE_TABLE);
        MappingManager manager = new MappingManager(session);
        exr = manager.createAccessor(ExchangerateAccessor.class);
        mapper = manager.mapper(Exchangerate.class);
    }

    public void handler(Message<String> message) {
        JsonObject data = new JsonObject(message.body());
        int baseCurrency = Integer.valueOf(data.getString("base"));
        int toCurrency = Integer.valueOf(data.getString("to"));

        logger.info("Query for base: {} to: {}", baseCurrency, toCurrency);
        ListenableFuture<Exchangerate> future = exr.getOne(baseCurrency, toCurrency,
                LocalDate.fromMillisSinceEpoch(System.currentTimeMillis()));
        Futures.addCallback(future, new FutureCallback<Exchangerate>() {
            @Override
            public void onSuccess(Exchangerate result) {
                if (result != null) {
                    message.reply(Json.encode(String.valueOf(result.getRate())));
                } else {
                    message.fail(404, message.body());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                message.fail(500, message.body());
            }
        });
    }

    public void handleCreate(Message<String> message) {
        JsonObject data = new JsonObject(message.body());
        int baseCurrency = Integer.valueOf(data.getString("base"));
        int toCurrency = Integer.valueOf(data.getString("to"));
        double rate = Double.valueOf(data.getString("rate"));
        LocalDate date = LocalDate.fromMillisSinceEpoch(System.currentTimeMillis()).add(Calendar.DAY_OF_MONTH, 1);
        
        logger.info("Insert base: {} to: {} rate: {} date: {}", baseCurrency, toCurrency, rate, date);
        
        Exchangerate entity = new Exchangerate();
        entity.setBaseCurrency(baseCurrency);
        entity.setToCurrency(toCurrency);
        entity.setRate(rate);
        entity.setEffectiveFrom(date);
        
        ListenableFuture<Void> future = mapper.saveAsync(entity);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                message.reply("OK");
            }

            @Override
            public void onFailure(Throwable t) {
                message.fail(500, message.body());
            }
        });
    }

}
