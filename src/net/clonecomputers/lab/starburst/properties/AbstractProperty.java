package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.google.common.reflect.*;

public abstract class AbstractProperty<T> extends AbstractPropertyTreeNode implements Property<T> {
	protected T value;
	protected boolean shouldRandomize = true;
	protected final boolean canRandomize;

	protected final TypeToken<T> type = new TypeToken<T>(getClass()){};
	
	protected JCheckBox shouldRandomizeCheckBox;
	
	public AbstractProperty(String name, String category, boolean canRandomize) {
		super(name, category);
		this.canRandomize = canRandomize;
	}
	
	/**
	 * MUST be called at end of constructor
	 */
	protected final void finishConstruction() {
		if(canRandomize) {
			randomize();
		}
		super.finishConstruction();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean isValid(Object newValue) {
		if(!type.isAssignableFrom(newValue.getClass())) return false;
		return isValidValue((T)newValue);
	}
	
	/**
	 * this returns true by default, but should be overriden if more advanced validity checks are needed
	 * @param newValue has already been checked to be of the right type
	 * @return whether newValue is a valid value
	 */
	public boolean isValidValue(T newValue) {
		return true;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setValue(Object newValue) {
		if(!isValid(newValue)) throw new IllegalArgumentException(newValue+" is not a valid value");
		value = (T)newValue;
	}

	@Override
	public boolean shouldRandomize() {
		return canRandomize && shouldRandomize;
	}

	@Override
	public void setShouldRandomize(boolean shouldRandomize) {
		this.shouldRandomize = shouldRandomize;
	}

	@Override
	public void maybeRandomize() {
		if(canRandomize && shouldRandomize) randomize();
	}
	
	public void toString0(StringBuilder sb, String indentString) {
		sb.append(indentString);
		sb.append("  value: ");
		sb.append(value);
		sb.append("\n");
		toString1(sb, indentString);
	}
	
	protected abstract void toString1(StringBuilder sb, String indentString);
	
	@Override
	public void refreshChangePanel() {
		if(canRandomize) {
			shouldRandomizeCheckBox.setSelected(shouldRandomize);
		}
	}
	
	@Override
	protected JComponent createChangePanel() {
		final JComponent primaryPanel = new JPanel(new BorderLayout());
		JComponent propertyPanel = new JPanel(new BorderLayout());
		JComponent nameAndRandomizePanel = new JPanel(new BorderLayout());
		JComponent propertySetterPanel = createPropertyPanel();
		JComponent centerPanel = createCenterPanel();
		if(canRandomize) {
			shouldRandomizeCheckBox = new JCheckBox();
			shouldRandomizeCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					shouldRandomize = shouldRandomizeCheckBox.isSelected();
				}
			});
			nameAndRandomizePanel.add(shouldRandomizeCheckBox, BorderLayout.LINE_START);
			final MouseListener unrandomizer = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getComponent().contains(e.getPoint()) && 
							!shouldRandomizeCheckBox.contains(e.getPoint())) {
						shouldRandomizeCheckBox.setSelected(false);
						shouldRandomize = false;
					} else {
						System.out.println(e.getPoint()+"Is out of bounds");
					}
				}
			};
			ContainerListener mouseListenerAdder = new ContainerAdapter() {
				@Override
				public void componentAdded(ContainerEvent e) {
					addListeners(e.getChild());
				}
				
				public void addListeners(Component c) {
					c.addMouseListener(unrandomizer);
					if(c instanceof Container){
						((Container) c).addContainerListener(this);
						for(Component child: ((Container)c).getComponents()) {
							addListeners(child);
						}
					}
				}
			};
			primaryPanel.addMouseListener(unrandomizer);
			primaryPanel.addContainerListener(mouseListenerAdder);
		}
		if(centerPanel != null) primaryPanel.add(centerPanel, BorderLayout.CENTER);
		nameAndRandomizePanel.add(new JLabel(name), BorderLayout.CENTER);
		propertyPanel.add(nameAndRandomizePanel, BorderLayout.LINE_START);
		propertyPanel.add(propertySetterPanel, BorderLayout.LINE_END);
		primaryPanel.add(propertyPanel, BorderLayout.PAGE_START);
		return primaryPanel;
	}
	
	protected abstract JComponent createPropertyPanel();
	protected abstract JComponent createCenterPanel();
}
