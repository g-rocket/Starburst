package net.clonecomputers.lab.starburst.properties;

import java.util.*;

import javax.swing.*;

public class BooleanProperty extends AbstractProperty<Boolean> {
	private final Random r;
	private JCheckBox checkBox;

	public BooleanProperty(String name, String category, boolean canRandomize, Random r) {
		super(name, category, canRandomize);
		this.r = r;
		finishConstruction();
	}

	@Override
	public void randomize() {
		value = r.nextBoolean();
	}

	@Override
	public void applyChangePanel() {
		value = checkBox.isSelected();
	}
	
	@Override
	public void refreshChangePanel() {
		checkBox.setSelected(value);
		super.refreshChangePanel();
	};

	@Override
	protected void toString1(StringBuilder sb, String indentString) {
		sb.append(indentString);
		sb.append(value);
		sb.append("\n");
	}

	@Override
	protected JComponent createPropertyPanel() {
		checkBox = new JCheckBox();
		return checkBox;
	}

	@Override
	protected JComponent createCenterPanel() {
		return null;
	}

}
