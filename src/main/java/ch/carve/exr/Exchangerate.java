package ch.carve.exr;

import com.datastax.driver.core.LocalDate;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "exr",
        name = "rates_query",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
public class Exchangerate {

	@PartitionKey(0)
	private int baseCurrency;
	@PartitionKey(1)
	private int toCurrency;
	@ClusteringColumn
	private LocalDate effectiveFrom;
	private double rate;

	public int getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(int baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public int getToCurrency() {
		return toCurrency;
	}

	public void setToCurrency(int toCurrency) {
		this.toCurrency = toCurrency;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(LocalDate date) {
		this.effectiveFrom = date;
	}

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

}
