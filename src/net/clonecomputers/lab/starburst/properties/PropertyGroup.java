package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.util.*;

import javax.swing.*;


public class PropertyGroup extends AbstractPropertyTreeNode implements PropertyTree {
	private final Map<String, PropertyTreeNode> subproperties;

	public PropertyGroup(String name, String category, Map<String, PropertyTreeNode> subproperties) {
		super(name, category);
		this.subproperties = subproperties;
		finishConstruction();
	}

	@Override
	public Map<String, PropertyTreeNode> subproperties() {
		return subproperties;
	}

	@Override
	public void toString0(StringBuilder sb, String indentString) {
		for(PropertyTreeNode p: subproperties.values()) {
			p.toString(indentString.length() + 2);
		}
	}

	@Override
	public void refreshChangePanel() {
		for(PropertyTreeNode subproperty: subproperties.values()) {
			subproperty.refreshChangePanel();
		}
	}

	@Override
	public void applyChangePanel() {
		for(PropertyTreeNode subproperty: subproperties.values()) {
			subproperty.applyChangePanel();
		}
	}

	@Override
	protected JComponent createChangePanel() {
		JComponent primaryPanel = new JPanel(new BorderLayout());
		JComponent labelPanel = new JPanel(new BorderLayout());
		labelPanel.add(new JLabel(name), BorderLayout.LINE_START);
		primaryPanel.add(labelPanel, BorderLayout.PAGE_START);
		JComponent subpropertiesPanel = new Box(BoxLayout.PAGE_AXIS);
		for(PropertyTreeNode p: subproperties.values()) {
			subpropertiesPanel.add(p.getChangePanel());
		}
		subpropertiesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), name));
		primaryPanel.add(subpropertiesPanel, BorderLayout.CENTER);
		return subpropertiesPanel;
	}

}
