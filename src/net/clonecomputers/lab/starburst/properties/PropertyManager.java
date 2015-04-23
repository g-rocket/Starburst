package net.clonecomputers.lab.starburst.properties;

import static net.clonecomputers.lab.starburst.util.StaticUtils.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.clonecomputers.lab.starburst.*;
import net.clonecomputers.lab.starburst.properties.types.*;
import ar.com.hjg.pngj.chunks.*;

import com.google.gson.*;

public class PropertyManager {
	private Random r;
	private Map<String, PropertyTreeNode> rootProperties = new LinkedHashMap<String, PropertyTreeNode>();
	private Map<String, Property<?>> allProperties = new HashMap<String, Property<?>>();
	private Set<String> categories = new HashSet<String>();
	private JComponent changeDialog = null;
	
	public static void main(String[] args) {
		VersionDependentMethodUtilities.initLookAndFeel();
		PropertyManager p = new PropertyManager();
		System.out.println(p.allProperties.keySet());
		p.rootNode().set("size.width", 1024);
		p.rootNode().set("size.height", 768);
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
	
	public PropertyManager() {
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
				synchronized (PropertyManager.this) {
					PropertyManager.this.notifyAll();
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
				synchronized (PropertyManager.this) {
					PropertyManager.this.notifyAll();
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
			rootProperties.put(toCamelCase(property.getKey()), parseProperty(property.getValue().getAsJsonObject(), 
					property.getKey(), toCamelCase(property.getKey()), null));
		}
	}
	
	public Map<String, PropertyTreeNode> parseSubproperties(JsonObject subproperties, String path, String category) {
		Map<String, PropertyTreeNode> parsedSubproperties = new LinkedHashMap<String, PropertyTreeNode>();
		for(Map.Entry<String, JsonElement> subproperty: subproperties.entrySet()) {
			if(subproperty.getKey().equalsIgnoreCase("category")) continue;
			PropertyTreeNode parsedSubproperty = parseProperty(
					subproperty.getValue().getAsJsonObject(),
					subproperty.getKey(),
					path + "." + toCamelCase(subproperty.getKey()),
					category);
			if(parsedSubproperty == null) continue;
			parsedSubproperties.put(toCamelCase(subproperty.getKey()), parsedSubproperty);
		}
		return parsedSubproperties;
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
			parsedProperty = new EnumProperty(name, category, property, r, path, this);
		} else if(type.equalsIgnoreCase("int")) {
			parsedProperty = new IntProperty(name, category, property, r);
		} else if(type.equalsIgnoreCase("double")) {
			parsedProperty = new DoubleProperty(name, category, property, r);
		} else if(type.equalsIgnoreCase("Color")) {
			parsedProperty = new ColorProperty(name, category, property, r);
		} else if(type.equalsIgnoreCase("boolean")) {
			parsedProperty = new BooleanProperty(name, category, property, r);
		} else {
			throw new IllegalStateException("invalid type: "+type);
		}
		allProperties.put(path, parsedProperty);
		return(parsedProperty);
	}
	
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
		meta.setText("net.clonecomputers.lab.starubrst.properties", exportToJson().toString(), true, true);
	}

	public void importFromPNG(List<PngChunk> chunks) {
		for(PngChunk c: chunks) {
			if (!ChunkHelper.isText(c)) continue;
			PngChunkTextVar ct = (PngChunkTextVar) c;
			String chunkKey = ct.getKey();
			if(!chunkKey.equals("net.clonecomputers.lab.starubrst.properties")) continue;
			String val = ct.getVal();
			importFromJson(new JsonParser().parse(val));
		}
	}
	
	public JsonElement exportToJson() {
		JsonObject json = new JsonObject();
		for(Map.Entry<String, Property<?>> p: allProperties.entrySet()) {
			json.add(p.getKey(), p.getValue().exportToJson());
		}
		return json;
	}
	
	public void importFromJson(JsonElement json) {
		for(Map.Entry<String, JsonElement> p: json.getAsJsonObject().entrySet()){
			allProperties.get(p.getKey()).importFromJson(p.getValue());
		}
	}

	public void randomize() {
		for(Property<?> p: allProperties.values()) {
			p.maybeRandomize();
		}
	}

	public PropertyTreeNode rootNode() {
		return new RootNode(rootProperties);
	}
}
