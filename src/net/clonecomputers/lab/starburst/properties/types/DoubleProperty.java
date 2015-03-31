package net.clonecomputers.lab.starburst.properties.types;

import static java.lang.Double.*;
import static java.lang.Math.*;
import net.clonecomputers.lab.starburst.properties.*;
import net.clonecomputers.lab.starburst.properties.random.*;

import com.google.gson.*;

public class DoubleProperty extends AbstractNumberProperty<Double> {
	private final int precision = 10000;
	private final double sliderPower = -1.5;
	
	public DoubleProperty(String name, String category, double min, double max, Randomizer<Double> r) {
		super(name, category, min, max, r);
		finishConstruction();
	}
	
	@Override
	protected int toSliderPos(Double v) {
		double p;
		if(isInfinite(min)) {
			if(isInfinite(max)) {
				p = signum(v) * pow(abs(v) + 1, 1/sliderPower);
				p = signum(p) - p;
				p = (p + 1)/2;
			} else {
				p = pow(max - v + 1, 1/sliderPower);
			}
		} else {
			if(isInfinite(max)) {
				p = pow(v - min + 1, 1/sliderPower);
				p = 1 - p;
			} else {
				p = (v - min) / (max - min);
			}
		}
		return (int)(p * precision);
	}
	
	@Override
	protected Double fromSliderPos(int sliderPos) {
		double p = sliderPos / (double)precision;
		if(isInfinite(min)) {
			if(isInfinite(max)) {
				p = 2*p - 1;
				p = signum(p) - p;
				return p == 0? 0: signum(p) * (pow(abs(p), sliderPower) - 1);
			} else {
				return max - pow(p, sliderPower) + 1;
			}
		} else {
			if(isInfinite(max)) {
				p = 1 - p;
				return min + pow(p, sliderPower) - 1;
			} else {
				return (max - min) * p + min;
			}
		}
	}

	@Override
	protected Double fromTextBox(String text) {
		return Double.parseDouble(text);
	}

	@Override
	protected String toTextBox(Double newValue) {
		String newText = String.format("%12f", newValue);
		newText = newText.trim();
		while(newText.length() < 12) newText += "0";
		return newText.substring(0,max(12, newText.indexOf(".")));
	}

	@Override
	protected void setupSlider() {
		slider.setMinimum(isInfinite(min)? 1: 0);
		slider.setMaximum(precision - (isInfinite(max)? 1: 0));
		slider.setPaintTicks(false);
		slider.setPaintLabels(false);
		slider.setSnapToTicks(false);
	}

	@Override
	public JsonObject exportToJson() {
		JsonObject json = super.exportToJson();
		json.add("value", doubleToJson(value));
		return json;
	}

	@Override
	public void importFromJson(JsonElement json) {
		super.importFromJson(json);
		setValue(jsonToDouble(json.getAsJsonObject().get("value").getAsJsonPrimitive()));
	}
}