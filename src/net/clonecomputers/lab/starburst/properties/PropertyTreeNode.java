package net.clonecomputers.lab.starburst.properties;

import java.util.*;

import javax.swing.*;

public interface PropertyTreeNode {
	public String getName();
	public String getCategory();
	
	public String toString(int indent);
	
	public JComponent getChangePanel();
	public void refreshChangePanel();
	public void applyChangePanel();
	
	public Map<String, PropertyTreeNode> subproperties();
	

	
	public PropertyTreeNode getSubproperty(String name);
	public <T> Property<? extends T> getSubproperty(Class<T> type, String name) throws ClassCastException;
	
	public <T> T get(Class<T> type, String name) throws ClassCastException;
	public boolean getAsBoolean(String name);
	public  double getAsDouble (String name);
	public   float getAsFloat  (String name);
	public    long getAsLong   (String name);
	public     int getAsInt    (String name);
	public   short getAsShort  (String name);
	public    byte getAsByte   (String name);
	public  String getAsString (String name);

	public boolean maybeSet(String name, Object value);
	public void set(String name, Object value);
	public boolean isValid(String name, Object value);
}
