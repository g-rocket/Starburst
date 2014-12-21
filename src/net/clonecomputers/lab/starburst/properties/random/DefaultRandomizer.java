package net.clonecomputers.lab.starburst.properties.random;

import static java.lang.Double.*;
import static java.lang.Math.*;

import java.util.*;

public class DefaultRandomizer<T extends Number> extends Randomizer<T> {
	
	public DefaultRandomizer(Class<T> type, Random r, double min, double max) {
		super(type, r, min, max);
	}
	
	public DefaultRandomizer(Random r, double min, double max) {
		super(r, min, max);
	}

	@Override
	protected Number randomize0() {
		if(isDiscrete) {
			if(Double.isInfinite(max)) {
				if(Double.isInfinite(min)) {
					return (int)(r.nextGaussian() * 10);
				} else {
					return (int)(min + Math.abs(r.nextGaussian()) * 10);
				}
			} else {
				if(Double.isInfinite(min)) {
					return (int)(max - Math.abs(r.nextGaussian() * 10));
				} else {
					return r.nextInt((int)(max - min)) + (int)min;
				}
			}
		} else {
			if(isInfinite(max)) {
				if(isInfinite(min)) {
					return r.nextGaussian();
				} else {
					return min + abs(r.nextGaussian());
				}
			} else {
				if(isInfinite(min)) {
					return max - abs(r.nextGaussian());
				} else {
					return (r.nextDouble() * (max - min)) + min;
				}
			}
		}
	}
	
}
