package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;

public abstract class AbstractPropertyTreeNode implements PropertyTreeNode {
	protected final String name;
	protected final String category;

	protected JComponent changePanel;

	public AbstractPropertyTreeNode(String name, String category) {
		this.name = name;
		this.category = category;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public String getCategory() {
		return category;
	}
	
	@Override
	public String toString() {
		return toString(0);
	}
	
	public String toString(int indent) {
		StringBuilder indentSB = new StringBuilder();
		for(int i = 0; i < indent; i++) indentSB.append(" ");
		String indentString = indentSB.toString();
		StringBuilder sb = new StringBuilder();
		sb.append(indentString);
		sb.append("\"");
		sb.append(name);
		sb.append("\": {\n");
		toString0(sb, indentString);
		sb.append(indentString);
		sb.append("}\n");
		return sb.toString();
	}
	
	public abstract void toString0(StringBuilder sb, String indentString);
	
	protected void finishConstruction() {
		
	}

	@Override
	public JComponent getChangePanel() {
		if(EventQueue.isDispatchThread()) {
			if(changePanel == null) {
				changePanel = createChangePanel();
			}
			refreshChangePanel();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						if(changePanel == null) {
							changePanel = createChangePanel();
						}
						refreshChangePanel();
					}
				});
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		return changePanel;
	}
	
	protected abstract JComponent createChangePanel();
}
