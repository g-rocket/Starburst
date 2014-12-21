package net.clonecomputers.lab.starburst.properties.random;

import static java.lang.Math.*;

import java.util.*;

public class BiasedRandomizer<T extends Number> extends Randomizer<T> {
	private final double bias;
	
	public BiasedRandomizer(Class<T> type, Random r, double min, double max, double bias) {
		super(type, r, min, max);
		this.bias = bias;
	}
	
	public BiasedRandomizer(Random r, double min, double max, double bias) {
		super(r, min, max);
		this.bias = bias;
	}
	
	@Override
	protected Number randomize0() {
		return pow(r.nextDouble(), log(bias)/log(0.5)) * (max - min) + min;
	}
	
}
