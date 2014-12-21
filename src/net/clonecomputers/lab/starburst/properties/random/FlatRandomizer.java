package net.clonecomputers.lab.starburst.properties.random;

import java.util.*;

public class FlatRandomizer<T extends Number> extends Randomizer<T> {
	public FlatRandomizer(Class<T> type, Random r, double min, double max) {
		super(type, r, min, max);
	}
	
	public FlatRandomizer(Random r, double min, double max) {
		super(r, min, max);
	}
	
	@Override
	protected Number randomize0() {
		return r.nextDouble() * (max - min) + min;
	}
	
}
