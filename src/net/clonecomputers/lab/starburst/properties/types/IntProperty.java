package net.clonecomputers.lab.starburst.properties.types;

import java.awt.event.*;
import java.util.*;

import net.clonecomputers.lab.starburst.properties.*;
import net.clonecomputers.lab.starburst.properties.random.*;

import com.google.gson.*;

public class IntProperty extends AbstractNumberProperty<Integer> {
	private final int smin, smax;
	
	public IntProperty(String name, String category, Random r, JsonObject data) {
		super(name, category, r, data);
		if(data.has("slider") && data.get("slider").isJsonArray()) {
			smin = data.get("slider").getAsJsonArray().get(0).getAsInt();
			smax = data.get("slider").getAsJsonArray().get(1).getAsInt();
		} else if(!data.get("slider").getAsBoolean()){
			smin = 1;
			smax = 1;
		} else {
			smin = 0;
			smax = 0;
		}
		finishConstruction();
		if(data.has("initialValue")) setValue(data.get("initialValue").getAsInt());
	}
	
	public IntProperty(String name, String category,
			double min, double max,
			int smin, int smax,
			Randomizer<Integer> r) {
		super(name, category, min, max, r);
		this.smin = smin;
		this.smax = smax;
		if((int)min != min && !Double.isInfinite(min)) throw new IllegalArgumentException("min must be an int or infinite");
		if((int)max != max && !Double.isInfinite(max)) throw new IllegalArgumentException("max must be an int or infinite");
		finishConstruction();
	}

	@Override
	protected void setupSlider() {
		if(smin == 0 && smax == 0) {
			// nothing was passed in, so we have to guess
			if(Double.isInfinite(max)) {
				if(Double.isInfinite(min)) {
					slider.setMinimum(-10);
					slider.setMaximum(10);
				} else {
					slider.setMinimum((int)min);
					slider.setMaximum(Math.max((int)min+5, 15));
				}
			} else {
				if(Double.isInfinite(min)) {
					slider.setMinimum(Math.min((int)min-5, -15));
					slider.setMaximum((int)max);
				} else {
					slider.setMinimum((int)min);
					slider.setMaximum((int)max);
				}
				slider.setMajorTickSpacing(5);
				slider.setMinorTickSpacing(1);
				slider.setSnapToTicks(true);
			}
		} else if(smin == 1 && smax == 1) {
			// no slider
			slider.setVisible(false);
			slider.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					IntProperty.this.slider.setVisible(false);
				}
			});
		} else {
			slider.setMinimum(smin);
			slider.setMaximum(smax);
			double dloghf = Math.floor(Math.log10(Math.pow((smax - smin), 2))) / 2;
			double major = dloghf - .5;
			double minor = dloghf - 1;
			if(Math.floor(major) != major) major += Math.log10(5) - .5;
			if(Math.floor(minor) != minor) minor += Math.log10(5) - .5;
			slider.setMajorTickSpacing((int)Math.pow(10,major));
			slider.setMinorTickSpacing((int)Math.pow(10,minor));
			slider.setSnapToTicks(minor == 0);
		}
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setPaintTrack(true);
	}

	@Override
	protected Integer fromSliderPos(int sliderPos) {
		return sliderPos;
	}

	@Override
	protected int toSliderPos(Integer value) {
		return value;
	}

	@Override
	protected Integer fromTextBox(String text) {
		return Integer.parseInt(text);
	}

	@Override
	protected String toTextBox(Integer newValue) {
		return Integer.toString(newValue);
	}
	
	@Override
	public JsonObject exportToJson() {
		JsonObject json = super.exportToJson();
		json.addProperty("value", value);
		return json;
	}
	
	@Override
	public void importFromJson(JsonElement json) {
		super.importFromJson(json);
		setValue(json.getAsJsonObject().get("value").getAsInt());
	}
}
