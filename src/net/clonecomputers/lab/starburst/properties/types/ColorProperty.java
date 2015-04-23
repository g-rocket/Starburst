package net.clonecomputers.lab.starburst.properties.types;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.clonecomputers.lab.starburst.properties.*;

import com.google.gson.*;

public class ColorProperty extends AbstractProperty<Color> {
	private final Random r;
	private JColorChooser colorChooser;
	
	public ColorProperty(String name, String category, JsonObject data, Random r) {
		super(name, category, data);
		this.r = r;
		finishConstruction();
		if(data.has("initialValue")) {
			String colorString = data.get("initialValue").getAsString();
			Color initialColor = Color.getColor(colorString);
			if(initialColor == null) initialColor = Color.decode(colorString);
			setValue(initialColor);
		}
	}
	
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

	@Override
	public JsonObject exportToJson() {
		JsonObject json = super.exportToJson();
		JsonObject color = new JsonObject();
		color.addProperty("red", value.getRed());
		color.addProperty("green", value.getGreen());
		color.addProperty("blue", value.getBlue());
		color.addProperty("alpha", value.getAlpha());
		json.add("value", color);
		return json;
	}

	@Override
	public void importFromJson(JsonElement json) {
		super.importFromJson(json);
		JsonObject color = json.getAsJsonObject().getAsJsonObject("value");
		int red = color.get("red").getAsInt();
		int green = color.get("green").getAsInt();
		int blue = color.get("blue").getAsInt();
		int alpha = color.get("alpha").getAsInt();
		setValue(new Color(red, green, blue, alpha));
	}
}
