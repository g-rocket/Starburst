package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.clonecomputers.lab.starburst.properties.random.*;

import com.google.common.reflect.*;
import com.google.gson.*;

public abstract class AbstractNumberProperty<T extends Number> extends AbstractProperty<T> {
	private final TypeToken<T> type = new TypeToken<T>(getClass()){};
	protected final Randomizer<T> r;
	protected double min;
	protected double max;
	protected JTextField textBox;
	protected JSlider slider;
	@SuppressWarnings("unchecked")
	private boolean isDiscrete = isDiscrete((Class<? extends Number>) type.getRawType());
	
	@SuppressWarnings("unchecked")
	public AbstractNumberProperty(String name, String category, JsonObject data, Random r) {
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
		if(isDiscrete) {
			if((!Double.isInfinite(max) && max != Math.round(max)) ||
			   (!Double.isInfinite(max) && max != Math.round(max))) {
				throw new IllegalArgumentException(String.format("Json passed to %s is invalid: max and min are not both integers: [%f,%f]", getClass().getCanonicalName(), max, min));
			}
		}
		this.r = Randomizer.createRandomizer((Class<T>)type.getRawType(), r, min, max, data.get("random"));
	}
	
	@Override
	public boolean isValid(Object newValue) {
		if(!(newValue instanceof Number)) return false;
		return isValidNumber((Number)newValue);
	}
	
	/**
	 * this returns if newValue is in range (with a double) by default,
	 * but should be overriden if more advanced validity checks are needed
	 * @param newValue has already been checked to be of the right type
	 * @return whether newValue is a valid value
	 */
	protected boolean isValidNumber(Number newValue) {
		if(isDiscrete(newValue.getClass()) != isDiscrete) return false;
		return min <= newValue.doubleValue() && newValue.doubleValue() <= max;
	}

	private static boolean isDiscrete(Class<? extends Number> c) {
		return !(c.isInstance(Double.class) || c.isInstance(Float.class));
	}
	
	@Override
	public boolean isValidValue(T newValue) {
		return (min <= newValue.doubleValue()) && (newValue.doubleValue() <= max);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object newValue) {
		if(!isValid(newValue)) {
			throw new IllegalArgumentException(newValue+" is not a valid value");
		}
		try {
			String typeName = type.unwrap().getRawType().getName();
			value = (T) Number.class.getMethod(typeName+"Value").invoke(newValue);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
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
	
	protected static JsonPrimitive doubleToJson(double value) {
		if(Double.isInfinite(value)) {
			if(value > 0) {
				return new JsonPrimitive("Inf");
			} else {
				return new JsonPrimitive("-Inf");
			}
		} else if(Double.isNaN(value)) {
			return new JsonPrimitive("NaN");
		} else {
			return new JsonPrimitive(value);
		}
	}
	
	protected static double jsonToDouble(JsonPrimitive json) {
		if(json.isString()) {
			String value = json.getAsString();
			if(value.equals("Inf")) {
				return Double.POSITIVE_INFINITY;
			} else if(value.equals("-Inf")) {
				return Double.NEGATIVE_INFINITY;
			} else if(value.equals("NaN")) {
				return Double.NaN;
			} else {
				throw new IllegalArgumentException(json+" is not a valid Double value");
			}
		} else {
			return json.getAsDouble();
		}
	}
	
	@Override
	public JsonObject exportToJson() {
		JsonObject json = super.exportToJson();
		json.add("min", doubleToJson(min));
		json.add("max", doubleToJson(max));
		return json;
	}
	
	@Override
	public void importFromJson(JsonElement json) {
		super.importFromJson(json);
		min = jsonToDouble(json.getAsJsonObject().get("min").getAsJsonPrimitive());
		max = jsonToDouble(json.getAsJsonObject().get("max").getAsJsonPrimitive());
	}
}
