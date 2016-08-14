package net.clonecomputers.lab.starburst.properties;



public abstract class AbstractPropertyTreeNode0 implements PropertyTreeNode {
	@Override
	public PropertyTreeNode getSubproperty(String name) {
		PropertyTreeNode node = this;
		for(String nextNodeName: name.split("\\.")) {
			node = node.subproperties().get(nextNodeName);
		}
		return node;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> Property<? extends T> getSubproperty(Class<T> type, String name) throws ClassCastException {
		return ((Property<? extends T>)getSubproperty(name));
	}
	
	
	
	@Override
	public <T> T get(Class<T> type, String name) throws ClassCastException {
		return getSubproperty(type, name).getValue();
	}

	@Override public boolean getAsBoolean(String name) { return get(boolean.class, name); }
	@Override public  double getAsDouble (String name) { return get( double.class, name); }
	@Override public   float getAsFloat  (String name) { return get(  float.class, name); }
	@Override public    long getAsLong   (String name) { return get(   long.class, name); }
	@Override public     int getAsInt    (String name) { return get(    int.class, name); }
	@Override public   short getAsShort  (String name) { return get(  short.class, name); }
	@Override public    byte getAsByte   (String name) { return get(   byte.class, name); }
	@Override public  String getAsString (String name) { return get( String.class, name); }
	
	
	
	public boolean maybeSet(String name, Object value) {
		Property<?> p = getSubproperty(Object.class, name);
		if(!p.isValid(value)) return false;
		p.setValue(value);
		return true;
	}
	
	public void set(String name, Object value) {
		getSubproperty(Object.class, name).setValue(value);
	}
	
	public boolean isValid(String name, Object value) {
		return getSubproperty(Object.class, name).isValid(value);
	}
	
}
