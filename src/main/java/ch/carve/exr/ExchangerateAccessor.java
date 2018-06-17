package ch.carve.exr;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;

@Accessor
public interface ExchangerateAccessor {

    @Query("select * from exr.rates_query where baseCurrency = ? and toCurrency = ? AND effectiveFrom <= '2018-06-14' limit 1")
    ListenableFuture<Exchangerate> getOne(int baseCurrency, int toCurrency);

}
