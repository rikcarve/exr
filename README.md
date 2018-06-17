# exr
A currency converter using vert.x and cassandra

### Preparation
#### Start cassandra
```bash
docker run -p 9042:9042 --name cassandra cassandra
```

#### Prepare data
```sql
CREATE KEYSPACE IF NOT EXISTS exr WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}
CREATE TABLE exr.rates_query (  
  baseCurrency int,  
  toCurrency int,
  effectiveFrom date, 
  rate double,  
  PRIMARY KEY ((baseCurrency, toCurrency), effectiveFrom)  
) WITH CLUSTERING ORDER BY ( effectivefrom DESC );

INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 840, '2018-06-12', 1.2);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 840, '2018-06-13', 1.3);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 840, '2018-06-14', 1.4);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 840, '2018-06-15', 1.5);

INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 978, '2018-06-12', 1.12);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 978, '2018-06-13', 1.13);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 978, '2018-06-14', 1.14);
INSERT INTO exr.rates_query (baseCurrency, toCurrency, effectiveFrom, rate) VALUES (756, 978, '2018-06-15', 1.15);
```
