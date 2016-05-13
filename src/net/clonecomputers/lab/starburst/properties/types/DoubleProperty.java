package net.clonecomputers.lab.starburst.properties.types;

import static java.lang.Double.*;
import static java.lang.Math.*;

import java.util.*;

import net.clonecomputers.lab.starburst.properties.*;
import net.clonecomputers.lab.starburst.properties.random.*;

import com.google.gson.*;

public class DoubleProperty extends AbstractNumberProperty<Double> {
	private final int precision = 10000;
	private final double sliderPower = -1.5;
	private final double smin, smax, sbias;
	
	public DoubleProperty(String name, String category, JsonObject data, Random r) {
		super(name, category, data, r);
		if(data.has("slider")) {
			if(data.get("slider").isJsonObject()) {
				if(data.getAsJsonObject("slider").has("range")) {
					smin = jsonToDouble(data.getAsJsonObject("slider").getAsJsonArray("range").get(0).getAsJsonPrimitive());
					smax = jsonToDouble(data.getAsJsonObject("slider").getAsJsonArray("range").get(1).getAsJsonPrimitive());
				} else {
					smin = min;
					smax = max;
				}
				if(data.getAsJsonObject("slider").has("type") &&
						data.getAsJsonObject("slider").get("type")
						.getAsString().equalsIgnoreCase("biased")) {
					sbias = data.getAsJsonObject("slider").get("bias").getAsDouble();
				} else {
					sbias = 1;
				}
			} else {
				smin = jsonToDouble(data.getAsJsonArray("slider").get(0).getAsJsonPrimitive());
				smax = jsonToDouble(data.getAsJsonArray("slider").get(1).getAsJsonPrimitive());
				sbias = 1;
			}
			if(smin < min || smax > max) {
				throw new IllegalArgumentException("Slider bounds are outside of allowed bounds");
			}
		} else {
			smin = min;
			smax = max;
			sbias = 1;
		}
		finishConstruction();
		if(data.has("initialValue")) setValue(data.get("initialValue").getAsDouble());
	}
	
	public DoubleProperty(String name, String category, double min, double max, Randomizer<Double> r) {
		super(name, category, min, max, r);
		smin = min;
		smax = max;
		sbias = 1;
		finishConstruction();
	}
	
	@Override
	protected int toSliderPos(Double v) {
		double p;
		if(isInfinite(smin)) {
			if(isInfinite(smax)) {
				p = signum(v) * pow(abs(v) + 1, 1/sliderPower);
				p = signum(p) - p;
				p = (p + 1)/2;
			} else {
				p = pow(smax - v + 1, 1/sliderPower);
			}
		} else {
			if(isInfinite(smax)) {
				p = pow(v - smin + 1, 1/sliderPower);
				p = 1 - p;
			} else {
				p = pow((v - smin) / (smax - smin), sbias);
			}
		}
		return (int)(p * precision);
	}
	
	@Override
	protected Double fromSliderPos(int sliderPos) {
		double p = sliderPos / (double)precision;
		if(isInfinite(smin)) {
			if(isInfinite(smax)) {
				p = 2*p - 1;
				p = signum(p) - p;
				return p == 0? 0: signum(p) * (pow(abs(p), sliderPower) - 1);
			} else {
				return smax - pow(p, sliderPower) + 1;
			}
		} else {
			if(isInfinite(smax)) {
				p = 1 - p;
				return smin + pow(p, sliderPower) - 1;
			} else {
				return (smax - smin) * pow(p + smin, 1/sbias);
			}
		}
	}

	@Override
	protected Double fromTextBox(String text) {
		return Double.parseDouble(text);
	}

	@Override
	protected String toTextBox(Double newValue) {
		String newText = String.format("%.12f", newValue);
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