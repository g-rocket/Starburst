package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.clonecomputers.lab.starburst.properties.random.*;

import com.google.gson.*;

public abstract class AbstractNumberProperty<T extends Number> extends AbstractProperty<T> {
	protected final Randomizer<T> r;
	protected double min;
	protected double max;
	protected JTextField textBox;
	protected JSlider slider;
	
	public AbstractNumberProperty(String name, String category, Random r, JsonObject data) {
		super(name, category, data);
		if(data.has("range")) {
			JsonElement jmin = data.get("range").getAsJsonArray().get(0);
			JsonElement jmax = data.get("range").getAsJsonArray().get(1);
			if(jmin.getAsJsonPrimitive().isString() && jmin.getAsString().equalsIgnoreCase("-Infinity")) {
				min = Double.NEGATIVE_INFINITY;
			} else {
				min = jmin.getAsDouble();
			}
			if(jmax.getAsJsonPrimitive().isString() && jmax.getAsString().equalsIgnoreCase("Infinity")) {
				max = Double.POSITIVE_INFINITY;
			} else {
				max = jmax.getAsDouble();
			}
		} else {
			min = Double.NEGATIVE_INFINITY;
			max = Double.POSITIVE_INFINITY;
		}
	}

	public AbstractNumberProperty(String name, String category, double min, double max, Randomizer<T> r) {
		super(name, category, r.canRandomize());
		if(min > max) throw new IllegalArgumentException("min > max");
		this.min = min;
		this.max = max;
		this.r = r;
	}
	
	@Override
	public void randomize() {
		if(r.canRandomize()) {
			value = r.randomize();
		}
	}
	
	@Override
	public boolean isValidValue(T newValue) {
		return (min <= newValue.doubleValue()) && (newValue.doubleValue() <= max);
	}

	@Override
	protected void toString1(StringBuilder sb, String indentString) {
		sb.append(indentString);
		sb.append("  range: [");
		sb.append(min);
		sb.append(", ");
		sb.append(max);
		sb.append("]\n");
	}

	@Override
	public void applyChangePanel() {
		setValue(fromTextBox(textBox.getText()));
	}

	@Override
	public void refreshChangePanel() {
		super.refreshChangePanel();
		slider.setValue(toSliderPos(value));
		textBox.setText(toTextBox(value));
	}

	protected abstract T fromSliderPos(int sliderPos);
	protected abstract int toSliderPos(T value);

	protected abstract T fromTextBox(String text);
	protected abstract String toTextBox(T newValue);

	protected abstract void setupSlider();

	@Override
	protected JComponent createPropertyPanel() {
		JComponent propertyPanel = new JPanel(new BorderLayout());
		textBox = new JTextField(8);
		final ActionListener textBoxActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				T newValue;
				try {
					newValue = fromTextBox(textBox.getText());
				} catch(NumberFormatException e) {
					System.err.println("Invalid value: not a number");
					textBox.setText(toTextBox(fromSliderPos(slider.getValue())));
					return;
				}
				if(!isValid(newValue)) {
					System.err.println("Invalid value: not within bounds");
					textBox.setText(toTextBox(fromSliderPos(slider.getValue())));
					return;
				}
				slider.setValue(toSliderPos(newValue));
				textBox.setText(toTextBox(newValue));
			}
		};
		textBox.addActionListener(textBoxActionListener);
		textBox.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				textBoxActionListener.actionPerformed(new ActionEvent(textBox, ActionEvent.ACTION_PERFORMED, ""));
			}

			@Override
			public void focusGained(FocusEvent e) {
				textBox.selectAll();
			}
		});
		slider = new JSlider();
		setupSlider();
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				textBox.setText(toTextBox(fromSliderPos(slider.getValue())));
			}
		});
		JPanel textBoxPanel = new JPanel(new FlowLayout());
		textBoxPanel.add(textBox);
		propertyPanel.add(textBoxPanel, BorderLayout.LINE_START);
		propertyPanel.add(slider, BorderLayout.LINE_END);
		return propertyPanel;
	}
	
	@Override
	public JComponent createCenterPanel() {
		return null;
	}
}
