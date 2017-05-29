package net.clonecomputers.lab.starburst;

import static java.lang.Math.*;
import static net.clonecomputers.lab.starburst.util.StaticUtils.*;

import java.awt.*;
import java.awt.Dialog.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;

import com.google.gson.*;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;
import net.clonecomputers.lab.starburst.finalize.*;
import net.clonecomputers.lab.starburst.properties.*;
import net.clonecomputers.lab.starburst.seed.*;
import net.clonecomputers.lab.starburst.util.*;

public class Starburst extends JDesktopPane {
	public Random rand = new Random();

	private BufferedImage canvas;
	private int[] pixels;
	public boolean current[][];
	public PixelOperationsList operations;
	private int[] pixelOrder;
	private int pixelNum;

	public Pair centerPair;
	private PropertyManager propertyManager;
	public PropertyTreeNode properties;

	public static final ExecutorService exec = Executors.newCachedThreadPool();
	private static final int THREADNUM = Runtime.getRuntime().availableProcessors() + 1;

	public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable(){
			@Override public void run(){
				Map<String, Object> flags = getArgs(args);
				System.out.println(flags);
				Dimension size;
				if(flags.containsKey("input") || flags.containsKey("i")) {
					System.out.println("Input dimensions");
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
					String arg0;
					try {
						arg0 = reader.readLine().trim();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					if(arg0.matches("\\d+\\s*[, ]\\s*\\d+")){ // WWW, HHH
						size = new Dimension(
								Integer.parseInt(arg0.split(",")[0].trim()),
								Integer.parseInt(arg0.split(",")[1].trim()));
					} else if(arg0.matches("\\d+")) { // WWW \n HHH
						String arg1;
						try {
							arg1 = reader.readLine().trim();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						size = new Dimension(
								Integer.parseInt(arg0.trim()),
								Integer.parseInt(arg1.trim()));
					} else {
						throw new IllegalArgumentException("Invalid dimensions");
					}
				} else if(flags.containsKey("size") || flags.containsKey("s")) {
					int[] sizeia = (int[])(flags.containsKey("size")? flags.get("size"): flags.get("s"));
					size = new Dimension(sizeia[0], sizeia[1]);
				} else if(flags.containsKey("width") || flags.containsKey("w")) {
					int width = (Integer)(flags.containsKey("width")? flags.get("width"): flags.get("w"));
					int height = (Integer)(flags.containsKey("height")? flags.get("height"): flags.get("h"));
					size = new Dimension(width, height);
				} else if(((String[])flags.get("args")).length == 2) {
					String[] args = (String[])flags.get("args");
					size = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
				} else if(((String[])flags.get("args")).length == 1) {
					String[] args = ((String[])flags.get("args"))[0].split(",");
					size = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
				} else {
					size = getSizeOfBiggestScreen();
				}
				Starburst s = new Starburst(size);
				if(flags.containsKey("many") || flags.containsKey("m")) {
					int howMany = (Integer)(flags.containsKey("many")? flags.get("many"): flags.get("m"));
					s.properties.set("renderDuringGeneration", false);
					s.genMany(".", howMany);
					return;
				}
				JFrame window = new JFrame();
				s.setupKeyAndClickListeners(window);
				window.setContentPane(s);
				VersionDependentMethodUtilities.enableFullscreen(window,false);
				window.setVisible(true);
				window.setSize(size);
				if(!(flags.containsKey("nonewimage"))) s.asyncNewImage();
			}
		});
	}

	private static Map<String, Object> getArgs(String[] args) {
		List<String> normalArgs = new ArrayList<String>();
		Map<String, Object> flags = new HashMap<String, Object>();
		for(int i = 0; i < args.length; i++) {
			if(args[i].startsWith("-")) {
				if(args[i].charAt(1) == '-') {
					int equalsPos = args[i].indexOf('=');
					if(equalsPos == -1) {
						flags.put(args[i].substring(2), null); // null indicates no value
					} else {
						flags.put(args[i].substring(2, equalsPos), parseValue(args[i].substring(equalsPos+1)));
					}
				} else {
					if(i+1 >= args.length || args[i+1].startsWith("-")) {
						flags.put(args[i].substring(1), null); // null indicates no value
					} else {
						flags.put(args[i].substring(1), parseValue(args[++i]));
					}
				}
			} else {
				normalArgs.add(args[i]);
			}
		}
		flags.put("args", normalArgs.toArray(new String[0]));
		return flags;
	}

	private static Object parseValue(String val) {
		try {
			return Integer.parseInt(val.trim());
		} catch(NumberFormatException nfe) {
			// not an int
		}
		try {
			return Double.parseDouble(val.trim());
		} catch(NumberFormatException nfe) {
			// not a double
		}
		if(val.contains(",")) {
			String[] vals = val.split(",");
			try {
				int[] retval = new int[vals.length];
				for(int i = 0; i < vals.length; i++) {
					retval[i] = Integer.parseInt(vals[i].trim());
				}
			} catch(NumberFormatException nfe) {
				// not an array of ints
			}
			try {
				double[] retval = new double[vals.length];
				for(int i = 0; i < vals.length; i++) {
					retval[i] = Double.parseDouble(vals[i].trim());
				}
			} catch(NumberFormatException nfe) {
				// not an array of doubles
			}
			return vals;
		}
		return val;
	}

	private static Dimension getSizeOfBiggestScreen() {
		GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		GraphicsDevice biggestScreen = null;
		int maxPixelsSoFar = 0;
		for(GraphicsDevice screen: screens){ // find biggest screen
			DisplayMode d = screen.getDisplayMode();
			if(d.getWidth() * d.getHeight() > maxPixelsSoFar){
				biggestScreen = screen;
				maxPixelsSoFar = d.getWidth() * d.getHeight();
			}
		}
		DisplayMode d = biggestScreen.getDisplayMode();
		return new Dimension(d.getWidth(), d.getHeight());
	}

	public void setupKeyAndClickListeners(JFrame window) {
		// consume System.in and redirect it to key listener
		// just in case the focus system breaks (as it so often does)
		// and you _really_need_ to save that awesome image
		exec.execute(new Runnable() {
			@Override public void run() {
				BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
				while(true) {
					try {
						String arg = r.readLine(); // wait for '\n'
						for(char c: arg.toCharArray()) {
							Starburst.this.keyPressed(c);
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		window.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e){
				Starburst.this.mousePressed();
			}
		});
		this.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e){
				Starburst.this.mousePressed();
			}
		});
		window.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent e){
				Starburst.this.keyPressed(e.getKeyChar());
			}
		});
		this.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent e){
				Starburst.this.keyPressed(e.getKeyChar());
			}
		});
	}

	public Starburst(int w,int h){
		propertyManager = new PropertyManager();
		properties = propertyManager.rootNode();
		setSize(w, h);
	}

	public Starburst(Dimension size) {
		this(size.width, size.height);
	}
	
	public void setSize(int w, int h) {
		properties.set("size.width", w);
		properties.set("size.height",h);
		canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		this.setPreferredSize(new Dimension(w,h));
		operations = new PixelOperationsList(w,h);
		current = new boolean[canvas.getWidth()][canvas.getHeight()];
		centerPair = new Pair(canvas.getWidth()/2, canvas.getHeight()/2);
	}

	private void genMany(String outputDirectory, int howMany) {
		for (int i=0;i<howMany;i++) {
			newImage();
			saveRandomName(outputDirectory);
		}
		System.exit(0);
	}

	private void saveRandomName(String outputDirectory) {
		String filename = String.format("%s/%s.png",
				outputDirectory,
				//properties.toString().replaceAll("\\s+", "").replace('.','_'),
				randomstr(24));
		save(new File(filename));
	}

	private String randomstr(int len) {
		StringBuilder str = new StringBuilder();
		for (int i=0;i<len;i++) {
			int thischar = rand.nextInt(62);
			if(thischar < 10){
				str.append((char)(thischar + '0'));
				continue;
			} else {
				thischar -= 10;
			}
			if(thischar < 26){
				str.append((char)(thischar + 'A'));
				continue;
			} else {
				thischar -= 26;
			}
			if(thischar < 26){
				str.append((char)(thischar + 'a'));
				continue;
			} else {
				thischar -= 26;
			}
		}
		return str.toString();
	}

	private void copyParamsFromFile(File file) {
		PngReader pngr = new PngReader(file);
		pngr.readSkippingAllRows(); // reads only metadata
		propertyManager.importFromPNG(pngr.getChunksList().getChunks());
		pngr.end(); // not necessary here, but good practice
	}

	private void save(File f) {
		String[] tmp = f.getName().split("[.]");
		String extension = tmp[tmp.length-1];
		if(!extension.equalsIgnoreCase("png")) {
			boolean writableExtension = false;
			for(String ext: ImageIO.getWriterFileSuffixes()) { // can ImageIO deal with it?
				if(extension.equalsIgnoreCase(ext)) {
					writableExtension = true;
					break;
				}
			}
			if(writableExtension) {
				//trying to write non-png
				//will not include metadata
				System.out.println(Arrays.toString(ImageIO.getWriterFormatNames()));
				try {
					ImageIO.write(canvas, extension.toLowerCase(), f);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					f = new File(f.toString().concat(".png"));
				}
			} else {
				f = new File(f.toString().concat(".png"));
			}
		}
		ImageInfo info = new ImageInfo(canvas.getWidth(), canvas.getHeight(),
				8, canvas.getColorModel().hasAlpha()); // I _think_ the bit-depth is 8
		PngWriter writer = new PngWriter(f, info);

		loadPixels();
		for(int row = 0; row < canvas.getHeight(); row++) {
			writer.writeRowInt(Arrays.copyOfRange(pixels, row*canvas.getWidth()*3, (row+1)*canvas.getWidth()*3));
		}

		PngMetadata meta = writer.getMetadata();
		meta.setTimeNow();
		/*meta.setText("Parameters",String.format("%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d, %d, %b",
				RBIAS, GBIAS, BBIAS, CENTERBIAS-1, GREYFACTOR,
				RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD, operations.getRemoveOrder().i(), SHARP));*/
		propertyManager.exportToPNG(meta);
		writer.end();
	}

	public void asyncNewImage() {
		exec.execute(new Runnable() {
			@Override public void run() {
				newImage();
			}
		});
	}

	public void newImage() {
		long sTime = System.currentTimeMillis();
		System.out.println("newImage");
		propertyManager.randomize();
		System.out.println(properties);
		//loadPixels();
		pixels = new int[canvas.getWidth() * canvas.getHeight() * canvas.getRaster().getNumBands()];
		if(properties.getAsBoolean("saveGenerationOrder")) {
			// should be *2, but I'm adding some extra space because it sometimes seems necessary
			pixelOrder = new int[canvas.getWidth() * canvas.getHeight() * 3];
			pixelNum = 0;
		} else {
			pixelOrder = null;
		}
		if(properties.getAsBoolean("renderDuringGeneration")) {
			savePixels();
		}
		falsifyCurrent();
		operations.setRemoveOrderBias(properties.getAsDouble("removeOrderBias"));
		seedImage(properties.getSubproperty(String.class, "seedMethod"));
		System.out.println("done seeding");
		fillOperations();
		generateImage();
		operations.clear();
		System.out.println("done generating");
		if(properties.getAsDouble("probabilityOfInclusion") < 1) {
			finalizeImage(properties.getSubproperty(String.class, "finalizationMethod"));
		}
		System.out.println("done finalizing");
		savePixels();
		System.out.println("end newImage");
		System.out.printf("took %d millis\n",System.currentTimeMillis()-sTime);
	}

	private void savePixels() {
		canvas.getRaster().setPixels(0, 0, canvas.getWidth(), canvas.getHeight(), pixels);
		this.repaint();
	}

	private void loadPixels() {
//		System.out.println(Runtime.getRuntime().freeMemory());
//		if(Runtime.getRuntime().freeMemory() < canvas.getWidth()*canvas.getHeight() * 4) {
//			System.out.print("Freeing Memory ... ");
//			pixels = null;
//			Runtime.getRuntime().gc();
//			System.out.println(Runtime.getRuntime().freeMemory());
//		}
		try {
			pixels = canvas.getRaster().getPixels(0, 0, canvas.getWidth(), canvas.getHeight(), pixels);
		} catch(OutOfMemoryError oom) {
			System.out.print("Freeing Memory ... ");
			pixels = null;
			Runtime.getRuntime().gc();
			loadPixels();
			System.out.println(Runtime.getRuntime().freeMemory());
		}
	}

	@Override public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		double scaleFactor = getScaleFactor();
		g2.scale(scaleFactor, scaleFactor);
		g2.drawImage(canvas, 0, 0, this);
	}
	
	private double getScaleFactor() {
		String scaleProp = properties.getAsString("scaling");
		if(scaleProp.equalsIgnoreCase("1x")) return 1;
		if(scaleProp.equalsIgnoreCase("2x")) return 0.5;
		if(scaleProp.equalsIgnoreCase("other")) return properties.getAsDouble("scaling.other");
		if(scaleProp.equalsIgnoreCase("Scale to fit")) {
			return Math.min(getWidth() / (double)canvas.getWidth(), getHeight() / (double)canvas.getHeight());
		}
		throw new IllegalArgumentException("Invalid scaling setting: "+scaleProp);
	}

	private void setParams() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JComponent changeDialog = propertyManager.getChangeDialog();
				final JDialog changeFrame = new JDialog();
				changeFrame.setModalityType(ModalityType.APPLICATION_MODAL);
				changeFrame.setContentPane(changeDialog);
				changeFrame.pack();
				//this.add(changeFrame, JLayeredPane.MODAL_LAYER);
				changeFrame.setLocation(
						Starburst.this.getWidth()/2 - changeFrame.getWidth()/2,
						Starburst.this.getHeight()/2 - changeFrame.getHeight()/2);
				System.out.println("waiting for changeDialog to close");
				changeFrame.setVisible(true);
				System.out.println("done!");
				operations.setRemoveOrderBias(properties.getAsDouble("removeOrderBias"));
				if(properties.getAsInt("size.width") != canvas.getWidth() ||
				   properties.getAsInt("size.height") != canvas.getHeight()) {
					setSize(properties.getAsInt("size.width"), properties.getAsInt("size.height"));
					System.out.println("set size to ("+properties.getAsInt("size.width")+","+properties.getAsInt("size.height")+")");
				}
			}
		});
		
	}
	
	private void keyPressed(char key) {
		key = String.valueOf(key).toLowerCase().charAt(0);
		switch(key) {
		case 'v':
			mousePressed();
			break;
		case 'p':
			exec.execute(new Runnable() {
				@Override
				public void run() {
					setParams();
				}
			});
			break;
		case 'c':
			exec.execute(new Runnable(){
				@Override
				public void run(){
					File input = chooseFile(JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
					if(input == null) {
						System.out.println("cancled");
						return;
					}
					copyParamsFromFile(input);
					newImage();
					//try to fix focus problems
					Starburst.this.requestFocusInWindow();
					Starburst.this.requestFocus();
					if(VersionDependentMethodUtilities.appleEawtAvailable()) {
						VersionDependentMethodUtilities.appleForeground();
					}
				}
			});
			break;
		case 'q':
			System.exit(0);
			break;
		case 'm':
			exec.execute(new Runnable(){@Override public void run(){
				String input = javax.swing.JOptionPane.showInternalInputDialog(Starburst.this,
						"How many images do you want to generate?");
				if (input == null) return;
				File outputDirectory = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.DIRECTORIES_ONLY);
				if(outputDirectory == null) return;
				if(isNamedAfterAncestor(outputDirectory)) {
					outputDirectory = outputDirectory.getParentFile();
				}
				if(!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}
				System.out.printf("about to generate %d images\n",Integer.parseInt(input));
				genMany(outputDirectory.getAbsolutePath(), Integer.parseInt(input));
			}});
			break;
		case 'f':
			JFrame window = (JFrame) SwingUtilities.getWindowAncestor(this);
			window.setVisible(false);
			window.dispose();
			if(window.isUndecorated()) {
				GraphicsDevice ourScreen = null;
				for(GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
					if(gd.getFullScreenWindow() != null && 
							gd.getFullScreenWindow().isAncestorOf(window) || window.isAncestorOf(gd.getFullScreenWindow()) || window.equals(gd.getFullScreenWindow())) {
						ourScreen = gd;
						break;
					}
				}
				if(ourScreen != null) ourScreen.setFullScreenWindow(null);
				window.setUndecorated(false);
			} else {
				GraphicsDevice biggestScreen = null;
				long maxPixels = Long.MIN_VALUE;
				for(GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
					long gdPixels = gd.getDisplayMode().getWidth() * gd.getDisplayMode().getHeight();
					if(gdPixels > maxPixels) {
						maxPixels = gdPixels;
						biggestScreen = gd;
					}
				}
				window.setUndecorated(true);
				biggestScreen.setFullScreenWindow(window);
			}
			window.pack();
			window.setVisible(true);
			break;
		case 'r':
			exec.execute(new Runnable() {
				@Override
				public void run() {
					renderVideo();
				}
			});
			break;
		case 'k':
			String curprops = propertyManager.exportToJson().toString();
			Object in = javax.swing.JOptionPane.showInputDialog(this, 
					"This string represents all of the image-generation settings.",
					"Set Parameters", JOptionPane.QUESTION_MESSAGE, null, null, curprops);
			if(in == null) return;
			String input = in.toString();
			if(input==null) return;
			propertyManager.importFromJson(new JsonParser().parse(input));
			break;
		case 'i':
			File shapeFile = chooseFile(JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY, true);
			if(shapeFile == null) {
				System.out.println("cancled");
				return;
			}
			try {
				importCutouts(shapeFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			break;
		default:
			if(key != 27) asyncNewImage();
		}
	}

	private void importCutouts(File shapeFile) throws FileNotFoundException, IOException {
		BufferedImage shapeImage = ImageIO.read(shapeFile);
		operations.clear();
		operations.setRemoveOrderBias(properties.getAsDouble("removeOrderBias"));
		pixels = new int[canvas.getWidth() * canvas.getHeight() * canvas.getRaster().getNumBands()];
		for(int x = 0; x < current.length; x++) {
			for(int y = 0; y < current[x].length; y++) {
				int color = shapeImage.getRGB(x, y);
				if((color & 0xff000000) > 0) {
					int i = (x+(y*canvas.getWidth()))*3;
					pixels[i]=red(color);
					pixels[i+1]=green(color);
					pixels[i+2]=blue(color);
					current[x][y] = true;
				} else {
					current[x][y] = false;
				}
				if((color & 0xff000000) == 0xff000000) {
					operations.addPoint(x, y);
				}
			}
		}
		if(properties.getAsBoolean("renderDuringGeneration")) {
			savePixels();
		}
		exec.execute(new Runnable() {
			@Override
			public void run() {
				generateImage();
				operations.clear();
				System.out.println("done generating");
				if(properties.getAsDouble("probabilityOfInclusion") < 1) {
					finalizeImage(properties.getSubproperty(String.class, "finalizationMethod"));
				}
				System.out.println("done finalizing");
				savePixels();
				System.out.println("done!");
			}
		});
	}

	private void renderVideo() {
		File outputDirectory = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.DIRECTORIES_ONLY);
		if(outputDirectory != null) {
			if(isNamedAfterAncestor(outputDirectory)) {
				outputDirectory = outputDirectory.getParentFile();
			}
			if(!outputDirectory.exists()) {
				outputDirectory.mkdirs();
			}
		}
		
		int maxPixel = pixelNum;
		properties.set("saveGenerationOrder", false);
		for(int frame = 0; true; frame++) {
			falsifyCurrent();
			for(int i = 0; (i*2) + 1 < maxPixel; i++) {
				current[pixelOrder[i*2]][pixelOrder[(i*2) + 1]] = true;
				fillPixel(pixelOrder[i*2], pixelOrder[(i*2) + 1]);
			}
			savePixels();
			if(outputDirectory != null) {
				File frameFile = new File(String.format("%s/frame-%04d.png",
						outputDirectory.getAbsolutePath(), frame));
				save(frameFile);
			}
		}
	}

	private static boolean isNamedAfterAncestor(File f) {
		File ancestor = f;
		do {
			ancestor = ancestor.getParentFile();
			if(ancestor.getName().equals(f.getName())) return true;
		} while(p(ancestor.list().length) <= 1);
		return false;
	}

	private static int p(int i) {
		System.out.println(i);
		return i;
	}
	
	private File chooseFile(final int dialogType, final int selectionMode) {
		return chooseFile(dialogType, selectionMode, false);
	}

	private File chooseFile(final int dialogType, final int selectionMode, boolean onAWTThread) {
		final JFileChooser fc = new JFileChooser();
		Runnable doAWTStuff = new Runnable() {
			@Override
			public void run() {
				fc.setCurrentDirectory(new java.io.File("."));
				fc.setFileSelectionMode(selectionMode);
				fc.setDialogType(dialogType);
				fc.setMultiSelectionEnabled(false);
				
				fc.showDialog(Starburst.this, null);
			}
		};
		if(onAWTThread) {
			doAWTStuff.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(doAWTStuff);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		return fc.getSelectedFile();
	}

	private void mousePressed() {
		System.out.println("saving image");
		exec.execute(new Runnable(){
			@Override public void run(){
				File output = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
				//Starburst.this.requestFocus();
				if(output == null) {
					// If a file was not selected
					System.out.println("No output file was selected...");
				} else {
					// If a file was selected, save image to path
					System.out.println("saving to "+output.getAbsolutePath());
					save(output);
					System.out.println("saved");
					// Now let's try to fix focus problems
//					Starburst.this.requestFocusInWindow();
//					Starburst.this.requestFocus();
//					if(VersionDependentMethodUtilities.appleEawtAvailable()) {
//						VersionDependentMethodUtilities.appleForeground();
//					}
				}
			}
		});
	}

	private void seedImage(Property<? extends String> how) {
		if(how.getValue().equals("Points")) {
			new PointSeeder(this).seedImage(how);
		} else if(how.getValue().equals("Lines")) {
			new LineSeeder(this).seedImage(how);
		} else {
			throw new IllegalArgumentException("Invalid seed method: "+how);
		}
	}

	

	public void fillOperations() {
		for (int x = 0; x < current.length; x++) {
			for (int y = 0; y < current[0].length; y++) {
				if (current[x][y]) {
					for(Pair p: getNeighbors(x, y)) {
						if(!current[p.x][p.y]) operations.addPoint(p.x, p.y); 
					}
				}
			}
		}
	}

	public int getPixel(int x, int y) {
		int i = (x+(y*canvas.getWidth()))*3;
		return color(pixels[i], pixels[i+1], pixels[i+2]);
	}

	public void setPixel(int x, int y, int c) {
		int i = (x+(y*canvas.getWidth()))*3;
		pixels[i]=red(c);
		pixels[i+1]=green(c);
		pixels[i+2]=blue(c);
		if(properties.getAsBoolean("renderDuringGeneration")) {
			canvas.setRGB(x, y, c);
			Graphics2D g = (Graphics2D) getGraphics();
			g.setColor(new Color(c));
			double scaleFactor = getScaleFactor();
			g.scale(scaleFactor, scaleFactor);
			g.drawLine(x, y, x, y);
//			g.drawImage(canvas, 
//					((int)(x*scaleFactor)), 
//					((int)(y*scaleFactor)), 
//					((int)(x*scaleFactor)+1), 
//					((int)(y*scaleFactor)+1), 
//					(int)((((int)(x*scaleFactor)))/scaleFactor), 
//					(int)((((int)(y*scaleFactor)))/scaleFactor), 
//					(int)((((int)(x*scaleFactor))+1)/scaleFactor), 
//					(int)((((int)(y*scaleFactor))+1)/scaleFactor), this);
		}
		if(properties.getAsBoolean("saveGenerationOrder")) {
			pixelOrder[pixelNum++] = x;
			pixelOrder[pixelNum++] = y;
		}
	}

	private void falsifyCurrent() {
		for (int i=0;i<canvas.getWidth();i++) {
			for (int j=0;j<canvas.getHeight();j++) {
				current[i][j]=false;
			}
		}
	}

	private void fillAllPixels() {
		while (true) {
			Pair myPair = operations.getPoint();
			if(myPair == null) {
				//if(operations.hasPoint()) continue;
				break;
			}
			int x = myPair.x, y = myPair.y;
			if (current[x][y]) continue;
			fillPixel(x, y);
			for(Pair p: getNeighbors(x, y)) {
				if(!current[p.x][p.y]){
					if(rand.nextDouble() < properties.getAsDouble("probabilityOfInclusion")) {
						operations.addPoint(p.x, p.y);
					}
				}
			}
		}
	}
	
	private Iterable<Pair> getNeighbors(int x, int y) {
		List<Pair> neighbors = new ArrayList<Pair>(4);
		if(x+1 < canvas.getWidth()) neighbors.add(new Pair(x+1, y));
		if(y-1 >= 0) neighbors.add(new Pair(x, y-1));
		if(x-1 >= 0) neighbors.add(new Pair(x-1, y));
		if(y+1 < canvas.getHeight()) neighbors.add(new Pair(x, y+1));
		neighbors.add(new Pair(x,y));
		return neighbors;
	}

	public void generateImage() {
		if(properties.getAsBoolean("multithreaded")) {
			List<Callable<Object>> threads = new ArrayList<Callable<Object>>();
			for (int i=0;i<THREADNUM;i++) {
				threads.add(new Callable<Object>() {
					public Object call() throws Exception {
						fillAllPixels();
						return null;
					}
				});
			}
			try {
				List<Future<Object>> futures = exec.invokeAll(threads);
				threads.clear();
				int i = 0;
				for(Future<Object> f: futures) {
					i++;
					final int j = i;
					final Future<Object> ff = f;
					threads.add(new Callable<Object>(){
						@Override
						public Object call() throws Exception {
							while(!ff.isDone()) {
								System.out.println(j+"is not done");
								Thread.sleep(100);
							}
							System.out.println(j+" is done!");
							ff.get();
							System.out.println(j+" is really done!");
							return null;
						}
					});
				}
				exec.invokeAll(threads);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			fillAllPixels();
		}
	}

	private void finalizeImage(Property<? extends String> how) {
		if(how.getValue().equals("Squares then loop (x,y)")) {
			new SquareFinalizer(this).finalizeImage(how);
		} else if(how.getValue().equals("Loop (x,y)")) {
			new LoopFromTopLeftFinalizer(this).finalizeImage(how);
		} else if(how.getValue().equals("Fill with solid color")) {
			new SolidFinalizer(this).finalizeImage(how);
		} else if(how.getValue().equals("Redo from point")) {
			new RegenFinalizer(this).finalizeImage(how);
		} else if(how.getValue().equals("Generate from outsides of holes")) {
			new GenerateFromOutsidesOfHolesFinalizer(this).finalizeImage(how);
		} else {
			throw new IllegalArgumentException("Don't know how to finalize by "+how);
		}
	}

	void printloc(int x, int y) {
		System.out.print("("+x+","+y+")");
	}

	/**
	 * Fills one pixel
	 * 
	 * it works by finding the neighbors that have been filled in a plus shape around it
	 * and then finding their maximum and minimum red green and blue values
	 * and then picks a (biased to center) random value between those for the pixel.
	 * @param x
	 * @param y
	 */
	public void fillPixel(int x, int y) {
		int maxr=255, minr=0;
		int maxg=255, ming=0;
		int maxb=255, minb=0;
		boolean hasNeighbors = false;
		for(Pair p: getNeighbors(x, y)) {
			if(!current[p.x][p.y]) continue;
			hasNeighbors = true;
			int curCol = getPixel(p.x, p.y);
			int curr=red(curCol), curg=green(curCol), curb=blue(curCol);
			if (maxr>curr) maxr=(curr+properties.getAsInt("pixelVariation.positiveVariation"));
			if (minr<curr) minr=(curr-properties.getAsInt("pixelVariation.negativeVariation"));
			if (maxg>curg) maxg=(curg+properties.getAsInt("pixelVariation.positiveVariation"));
			if (ming<curg) ming=(curg-properties.getAsInt("pixelVariation.negativeVariation"));
			if (maxb>curb) maxb=(curb+properties.getAsInt("pixelVariation.positiveVariation"));
			if (minb<curb) minb=(curb-properties.getAsInt("pixelVariation.negativeVariation"));
		}
		if(!hasNeighbors) {
			setPixel(x, y, randomColor(rand));
			current[x][y] = true;
			return;
		}
		int r=biasedRandom(minr, maxr, properties.getAsDouble("centerBias"), properties.getAsDouble("colorBiases.redBias"));
		int g=biasedRandom(ming, maxg, properties.getAsDouble("centerBias"), properties.getAsDouble("colorBiases.greenBias"));
		int b=biasedRandom(minb, maxb, properties.getAsDouble("centerBias"), properties.getAsDouble("colorBiases.blueBias"));
		if(properties.getAsBoolean("preventOverflows")) {
			r = clamp(0,r,255);
			g = clamp(0,g,255);
			b = clamp(0,b,255);
		}
		setPixel(x, y, color(r, g, b));
		current[x][y]=true;
	}
	
	public int getImageWidth() {
		return canvas.getWidth();
	}
	
	public int getImageHeight() {
		return canvas.getHeight();
	}

	int biasedRandom(int minVal, int maxVal, double biastocenter, double biasFactor) {

		if (maxVal<minVal) {
			if (properties.getAsString("oorResolution").equals("Randomly choose one side")) {
				return rand.nextBoolean()? minVal: maxVal;
			} else {
				return ((maxVal+minVal)/2);
			}
		}

		double a = .5 + (rand.nextBoolean()? -.5: .5)*pow(rand.nextDouble(), biastocenter);
		double val = (a*(double)(maxVal-minVal+biasFactor+1))+minVal;
		return (int)val;
	}

}
