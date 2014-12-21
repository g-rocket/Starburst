package net.clonecomputers.lab.starburst.properties;


public interface Property<T> extends PropertyTreeNode {
	public T getValue();
	public void setValue(Object value);
	public boolean isValid(Object newValue);
	
	public boolean shouldRandomize();
	public void setShouldRandomize(boolean shouldRandomize);
	public void maybeRandomize();
	public void randomize();
}
