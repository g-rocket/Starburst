package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.util.*;

import javax.swing.*;

public class ColorProperty extends AbstractProperty<Color> {
	private final Random r;
	private JColorChooser colorChooser;
	
	public ColorProperty(String name, String category, boolean shouldRandomize, Random r) {
		super(name, category, shouldRandomize);
		this.r = r;
		finishConstruction();
	}

	@Override
	public boolean isValidValue(Color newValue) {
		return true;
	}

	@Override
	public void randomize() {
		value = new Color(r.nextInt(0xffffff));
	}
	
	@Override
	protected void toString1(StringBuilder sb, String indentString) {}

	@Override
	public void applyChangePanel() {
		setValue(colorChooser.getColor());
	}

	@Override
	public void refreshChangePanel() {
		super.refreshChangePanel();
		colorChooser.setColor(value);
	}

	@Override
	protected JComponent createPropertyPanel() {
		colorChooser = new JColorChooser(value);
		return colorChooser;
	}

	@Override
	protected JComponent createCenterPanel() {
		return null;
	}
}
