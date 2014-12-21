package net.clonecomputers.lab.starburst.properties.random;


public class NoRandomizer<T extends Number> extends Randomizer<T> {
	
	public NoRandomizer() {
		super(null, Double.NaN, Double.NaN);
	}
	
	public NoRandomizer(Class<T> type) {
		super(type, null, Double.NaN, Double.NaN);
	}
	
	public boolean canRandomize() {
		return false;
	}

	@Override
	protected Number randomize0() {
		throw new IllegalStateException("NoRandomizer can't randomize");
	}
}
