package net.clonecomputers.lab.starburst.properties.random;

import java.util.*;

public class GaussianRandomizer<T extends Number> extends Randomizer<T> {
	private final double mean, variance;
	
	public GaussianRandomizer(Random r, double mean, double variance) {
		this(r, mean, variance, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	
	public GaussianRandomizer(Class<T> type, Random r, double mean, double variance) {
		this(type, r, mean, variance, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}
	
	public GaussianRandomizer(Random r, double min, double max, double mean, double variance) {
		super(r, min, max);
		this.mean = mean;
		this.variance = variance;
	}
	
	public GaussianRandomizer(Class<T> type, Random r, double min, double max, double mean, double variance) {
		super(type, r, min, max);
		this.mean = mean;
		this.variance = variance;
	}

	@Override
	protected Number randomize0() {
		return Math.min(Math.max(min, mean + (variance * r.nextGaussian())), max);
	}
	
}
