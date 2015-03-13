package net.clonecomputers.lab.starburst.properties;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.random.*;
import ar.com.hjg.pngj.chunks.*;

import com.google.gson.*;

public class Properties {
	private Random r;
	private Map<String, PropertyTreeNode> rootProperties = new LinkedHashMap<String, PropertyTreeNode>();
	private Map<String, Property<?>> allProperties = new HashMap<String, Property<?>>();
	private Set<String> categories = new HashSet<String>();
	private JComponent changeDialog = null;
	
	public static void main(String[] args) {
		VersionDependentMethodUtilities.initLookAndFeel();
		Properties p = new Properties();
		System.out.println(p.allProperties.keySet());
		p.set("size.width", 1024);
		p.set("size.height", 768);
		JFrame window = new JFrame("Properties Test");
		window.setContentPane(p.getChangeDialog());
		window.pack();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
	}
	
	public JComponent getChangeDialog() {
		if(changeDialog == null) createDialog();
		return changeDialog;
	}
	
	public Properties() {
		JsonObject config = new JsonParser().parse(new InputStreamReader(getClass().getResourceAsStream("Properties.json"))).getAsJsonObject();
		r = new Random();
		parse(config);
	}
	
	private void createDialog() {
		changeDialog = new JPanel();
		changeDialog.setLayout(new BorderLayout());
		final JTabbedPane tabsPane = new JTabbedPane();
		Map<String, JComponent> tabs = new HashMap<String, JComponent>();
		changeDialog.add(tabsPane, BorderLayout.CENTER);
		for(PropertyTreeNode p: rootProperties.values()) {
			JComponent propertyPanel = p.getChangePanel();
			if(tabs.get(p.getCategory()) == null) {
				JComponent tab = new Box(BoxLayout.PAGE_AXIS);
				tabs.put(p.getCategory(), tab);
				JPanel realTab = new JPanel(new BorderLayout());
				realTab.add(tab, BorderLayout.PAGE_START);
				tabsPane.add(p.getCategory(), realTab);
			}
			tabs.get(p.getCategory()).add(propertyPanel);
		}
		
		JComponent okCancel = new Box(BoxLayout.LINE_AXIS);
		JButton randomizeButton = new JButton("Randomize");
		randomizeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for(Property<?> p: allProperties.values()) {
					if(p.getCategory().equals(tabsPane.getTitleAt(tabsPane.getSelectedIndex()))) {
						p.maybeRandomize();
						p.refreshChangePanel();
					}
				}
			}
		});
		okCancel.add(randomizeButton);
		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for(Property<?> p: allProperties.values()) {
					p.refreshChangePanel();
				}
			}
		});
		okCancel.add(resetButton);
		JButton applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for(Property<?> p: allProperties.values()) {
					p.applyChangePanel();
				}
			}
		});
		okCancel.add(applyButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Component window = SwingUtilities.getAncestorOfClass(RootPaneContainer.class, changeDialog);
				window.setVisible(false);
				/*
				if(window instanceof Window) {
					window.dispatchEvent(new WindowEvent((Window)window, WindowEvent.WINDOW_CLOSING));
				} else if(window instanceof JInternalFrame) {
					try {
						((JInternalFrame)window).setClosed(true);
					} catch (PropertyVetoException e1) {
						System.err.println("the frame doesn't seem to want to close: ");
						e1.printStackTrace();
					}
				} else {
					throw new IllegalStateException("I can't close a "+window.getClass().getSimpleName());
				}
				*/
				synchronized (Properties.this) {
					Properties.this.notifyAll();
				}
			}
		});
		okCancel.add(cancelButton);
		cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ESCAPE"), "pressed");
		cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ESCAPE"), "released");
		final JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for(Property<?> p: allProperties.values()) {
					p.applyChangePanel();
				}
				Component window = SwingUtilities.getAncestorOfClass(RootPaneContainer.class, changeDialog);
				window.setVisible(false);
				/*if(window instanceof Window) {
					window.dispatchEvent(new WindowEvent((Window)window, WindowEvent.WINDOW_CLOSING));
				} else if(window instanceof JInternalFrame) {
					try {
						((JInternalFrame)window).setClosed(true);
					} catch (PropertyVetoException e1) {
						System.err.println("the frame doesn't seem to want to close: ");
						e1.printStackTrace();
					}
				} else {
					throw new IllegalStateException("I can't close a "+window.getClass().getSimpleName());
				}*/
				synchronized (Properties.this) {
					Properties.this.notifyAll();
				}
			}
		});
		okCancel.add(okButton);
		Container bottom = new JPanel(new BorderLayout());
		bottom.add(okCancel, BorderLayout.LINE_END);
		changeDialog.add(bottom, BorderLayout.PAGE_END);
		changeDialog.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public void hierarchyChanged(HierarchyEvent arg0) {
				RootPaneContainer rpc = ((RootPaneContainer) SwingUtilities.getAncestorOfClass(RootPaneContainer.class, changeDialog));
				if(rpc != null) {
					rpc.getRootPane().setDefaultButton(okButton);
				}
			}
		});
	}

	private void parse(JsonObject config) {
		for(Map.Entry<String, JsonElement> property: config.entrySet()) {
			rootProperties.put(property.getKey(), parseProperty(property.getValue().getAsJsonObject(), 
					property.getKey(), toCamelCase(property.getKey()), null));
		}
	}
	
	private Map<String, PropertyTreeNode> parseSubproperties(JsonObject subproperties, String path, String category) {
		Map<String, PropertyTreeNode> parsedSubproperties = new LinkedHashMap<String, PropertyTreeNode>();
		for(Map.Entry<String, JsonElement> subproperty: subproperties.entrySet()) {
			if(subproperty.getKey().equalsIgnoreCase("category")) continue;
			PropertyTreeNode parsedSubproperty = parseProperty(
					subproperty.getValue().getAsJsonObject(),
					subproperty.getKey(),
					path + "." + toCamelCase(subproperty.getKey()),
					category);
			if(parsedSubproperty == null) continue;
			parsedSubproperties.put(subproperty.getKey(), parsedSubproperty);
		}
		return parsedSubproperties;
	}

	static Pattern hyphenRegex = Pattern.compile("(?<=^|\\s)([^\\s]*-[^\\s]*)(?=$|\\s)");
	static Pattern spaceRegex = Pattern.compile("\\s+(\\w)");
	private static String toCamelCase(String s) {
		StringBuffer dehyphenated = new StringBuffer();
		Matcher hyphenMatcher = hyphenRegex.matcher(s);
		while(hyphenMatcher.find()) {
			hyphenMatcher.appendReplacement(dehyphenated, "");
			String[] words = s.substring(hyphenMatcher.start(), hyphenMatcher.end()).split("-");
			for(String word: words) {
				dehyphenated.append(word.charAt(0));
			}
		}
		hyphenMatcher.appendTail(dehyphenated);
		s = dehyphenated.toString();
		s = s.toLowerCase();
		StringBuffer camelcase = new StringBuffer();
		Matcher spaceMatcher = spaceRegex.matcher(s);
		while(spaceMatcher.find()) {
			spaceMatcher.appendReplacement(camelcase, spaceMatcher.group(1).toUpperCase());
		}
		spaceMatcher.appendTail(camelcase);
		s = camelcase.toString();
		return s;
	}
	
	private PropertyTreeNode parseProperty(JsonObject property, String name, String path, String category) {
		Property<?> parsedProperty;
		if(property.has("disabled") && property.get("disabled").getAsBoolean()) return null;
		if(property.has("category")) {
			category = property.get("category").getAsString();
		} else if(category == null) {
			category = "default";
		}
		if(!property.has("type")) {
			return new PropertyGroup(name, category, parseSubproperties(property, path, category));
		}
		boolean canRandomize = 
				!(property.has("random") &&
				property.get("random").isJsonPrimitive() &&
				property.get("random").getAsJsonPrimitive().isBoolean() &&
				!property.get("random").getAsBoolean());
		String type = property.get("type").getAsString();
		categories.add(category);
		if(type.equalsIgnoreCase("enum")) {
			Map<String, Map<String, PropertyTreeNode>> values = new LinkedHashMap<String, Map<String, PropertyTreeNode>>();
			for(Map.Entry<String, JsonElement> value: property.getAsJsonObject("values").entrySet()) {
				if(value.getValue().getAsJsonObject().has("disabled") &&
						value.getValue().getAsJsonObject().get("disabled").getAsBoolean()) continue;
				values.put(value.getKey(), parseSubproperties(value.getValue().getAsJsonObject(),
						path + "." + toCamelCase(value.getKey()), category));
			}
			parsedProperty = new EnumProperty(name, category, values, canRandomize, r);
			if(property.has("initialValue")) parsedProperty.setValue(property.get("initialValue").getAsString());
		} else if(type.equalsIgnoreCase("int")) {
			double min, max;
			if(property.has("range")) {
				JsonElement jmin = property.get("range").getAsJsonArray().get(0);
				JsonElement jmax = property.get("range").getAsJsonArray().get(1);
				if(jmin.getAsJsonPrimitive().isString() && jmin.getAsString().equalsIgnoreCase("-Infinity")) {
					min = Double.NEGATIVE_INFINITY;
				} else {
					min = jmin.getAsInt();
				}
				if(jmax.getAsJsonPrimitive().isString() && jmax.getAsString().equalsIgnoreCase("Infinity")) {
					max = Double.POSITIVE_INFINITY;
				} else {
					max = jmax.getAsInt();
				}
			} else {
				min = Double.NEGATIVE_INFINITY;
				max = Double.POSITIVE_INFINITY;
			}
			int smin, smax; // for the slider
			if(property.has("slider") && property.get("slider").isJsonArray()) {
				smin = property.get("slider").getAsJsonArray().get(0).getAsInt();
				smax = property.get("slider").getAsJsonArray().get(1).getAsInt();
			} else if(!property.get("slider").getAsBoolean()){
				smin = 1;
				smax = 1;
			} else {
				smin = 0;
				smax = 0;
			}
			Randomizer<Integer> rand = Randomizer.createRandomizer(Integer.class, r, min, max, property.get("random"));
			parsedProperty = new IntProperty(name, category, min, max, smin, smax, rand);
			if(property.has("initialValue")) parsedProperty.setValue(property.get("initialValue").getAsInt());
		} else if(type.equalsIgnoreCase("double")) {
			double min, max;
			if(property.has("range")) {
				JsonElement jmin = property.get("range").getAsJsonArray().get(0);
				JsonElement jmax = property.get("range").getAsJsonArray().get(1);
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
			Randomizer<Double> rand = Randomizer.createRandomizer(Double.class, r, min, max, property.get("random"));
			parsedProperty = new DoubleProperty(name, category, min, max, rand);
			if(property.has("initialValue")) parsedProperty.setValue(property.get("initialValue").getAsDouble());
		} else if(type.equalsIgnoreCase("Color")) {
			parsedProperty = new ColorProperty(name, category, canRandomize, r);
			if(property.has("initialValue")) {
				String colorString = property.get("initialValue").getAsString();
				Color initialColor = Color.getColor(colorString);
				if(initialColor == null) initialColor = Color.decode(colorString);
				parsedProperty.setValue(initialColor);
			}
		} else if(type.equalsIgnoreCase("boolean")) {
			parsedProperty = new BooleanProperty(name, category, canRandomize, r);
			if(property.has("initialValue")) {
				String initialString = property.get("initialValue").getAsString();
				boolean initialValue = Boolean.parseBoolean(initialString);
				parsedProperty.setValue(initialValue);
			}
		} else {
			throw new IllegalStateException("invalid type: "+type);
		}
		allProperties.put(path, parsedProperty);
		return(parsedProperty);
	}
	
	public boolean maybeSet(String name, Object value) {
		Property<?> p = allProperties.get(name);
		if(!p.isValid(value)) return false;
		p.setValue(value);
		return true;
	}
	
	public void set(String name, Object value) {
		allProperties.get(name).setValue(value);
	}
	
	public boolean isValid(String name, Object value) {
		return allProperties.get(name).isValid(value);
	}
	
	public Object get(String name) {
		return allProperties.get(name).getValue();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type, String name) throws ClassCastException {
		return (T) allProperties.get(name).getValue();
	}
	
	public boolean getAsBoolean(String name) { return get(boolean.class, name); }
	public  double getAsDouble (String name) { return get( double.class, name); }
	public   float getAsFloat  (String name) { return get(  float.class, name); }
	public    long getAsLong   (String name) { return get(   long.class, name); }
	public     int getAsInt    (String name) { return get(    int.class, name); }
	public   short getAsShort  (String name) { return get(  short.class, name); }
	public    byte getAsByte   (String name) { return get(   byte.class, name); }
	public  String getAsString (String name) { return get( String.class, name); }
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(Map.Entry<String,Property<?>> p: allProperties.entrySet()) {
			sb.append(p.getKey());
			sb.append(": \n");
			sb.append(p.getValue().toString(2));
		}
		return sb.toString();
	}

	public void exportToPNG(PngMetadata meta) {
		meta.setText("net.clonecomputers.lab.Starburst.propertyTree", rootProperties.toString(), true, true);
		meta.setText("net.clonecomputers.lab.Starburst.properties", allProperties.toString(), true, true);
	}

	public void importFromPNG(List<PngChunk> chunks) {
		throw new UnsupportedOperationException("Not implemented yet"); //TODO: implement me
	}

	public void randomize() {
		for(Property<?> p: allProperties.values()) {
			p.maybeRandomize();
		}
	}
}
