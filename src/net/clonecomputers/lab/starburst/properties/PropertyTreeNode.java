package net.clonecomputers.lab.starburst.properties;

import javax.swing.*;

public interface PropertyTreeNode {
	public String getName();
	public String getCategory();
	
	public String toString(int indent);
	
	public JComponent getChangePanel();
	public void refreshChangePanel();
	public void applyChangePanel();
}
