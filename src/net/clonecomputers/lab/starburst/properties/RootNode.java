package net.clonecomputers.lab.starburst.properties;

import java.util.*;

import javax.swing.*;

public class RootNode extends AbstractPropertyTreeNode0 implements PropertyTreeNode {
	private Map<String, PropertyTreeNode> subproperties;

	public RootNode(Map<String, PropertyTreeNode> subproperties) {
		this.subproperties = subproperties;
	}

	@Override
	public String getName() {
		return "properties";
	}

	@Override
	public String getCategory() {
		return "";
	}

	@Override
	public String toString(int indent) {
		StringBuffer sb = new StringBuffer();
		for(Map.Entry<String,PropertyTreeNode> p: subproperties.entrySet()) {
			sb.append(p.getKey());
			sb.append(": \n");
			sb.append(p.getValue().toString(2));
		}
		return sb.toString();
	}

	@Override
	public JComponent getChangePanel() {
		throw new UnsupportedOperationException("the root node has no change panel");
	}

	@Override
	public void refreshChangePanel() {
		for(PropertyTreeNode p: subproperties.values()) {
			p.refreshChangePanel();
		}
	}

	@Override
	public void applyChangePanel() {
		for(PropertyTreeNode p: subproperties.values()) {
			p.applyChangePanel();
		}
	}

	@Override
	public Map<String, PropertyTreeNode> subproperties() {
		return subproperties;
	}

}
