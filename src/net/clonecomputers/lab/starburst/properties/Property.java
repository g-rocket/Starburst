package net.clonecomputers.lab.starburst.properties;

import com.google.gson.*;


public interface Property<T> extends PropertyTreeNode {
	public T getValue();
	public void setValue(Object value);
	public boolean isValid(Object newValue);
	
	public boolean shouldRandomize();
	public void setShouldRandomize(boolean shouldRandomize);
	public void maybeRandomize();
	public void randomize();
	
	public JsonElement exportToJson();
	public void importFromJson(JsonElement json);
}
