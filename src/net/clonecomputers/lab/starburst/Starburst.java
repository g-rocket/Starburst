package net.clonecomputers.lab.starburst;

import static java.lang.Math.*;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;

import net.clonecomputers.lab.starburst.properties.Properties;
import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;

public class Starburst extends JDesktopPane {
	private Random myRandom = new Random();

	private BufferedImage canvas;
	private int[] pixels;
	private boolean current[][];
	private PixelOperationsList operations;

	private Pair centerPair;
	private Properties properties = new Properties();

	public static final ExecutorService exec = Executors.newCachedThreadPool();
	private static final int THREADNUM = Runtime.getRuntime().availableProcessors();

	public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
		SwingUtilities.invokeAndWait(new Runnable(){
			@Override public void run(){
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
				JFrame window = new JFrame();
				Dimension size;
				int howManyNumbers = 0;
				String[] dims = new String[2];
				for(int i = 0; i < args.length; i++) {
					if(isInt(args[i])) dims[howManyNumbers++] = args[i];
					args[i] = null;
				}
				Arrays.sort(args);
				switch(howManyNumbers) {
				case 0:
					size = new Dimension(d.getWidth(), d.getHeight());
					break;
				case 1:
					size = new Dimension(
							Integer.parseInt(args[0].split(",")[0].trim()),
							Integer.parseInt(args[0].split(",")[1].trim()));
					break;
				case 2:
					size = new Dimension(
							Integer.parseInt(args[0].trim()),
							Integer.parseInt(args[1].trim()));
					break;
				default:
					throw new IllegalArgumentException("too many numbers");
				}
				if(Arrays.binarySearch(args, "inputdims") > 0) {
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
				}
				Starburst s = new Starburst(size);
				s.setupKeyAndClickListeners(window);
				window.setContentPane(s);
				VersionDependentMethodUtilities.enableFullscreen(window,biggestScreen,false);
				window.setVisible(true);
				window.setSize(size);
				s.asyncNewImage();
			}
		});
	}

	private static boolean isInt(String arg) {
		for(char c: arg.toCharArray()) {
			if(c < '0' || c > '9') return false;
		}
		return true;
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
		canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		this.setPreferredSize(new Dimension(w,h));
		operations = new PixelOperationsList(w,h);
		properties = new Properties();
		properties.set("size.width", w);
		properties.set("size.height",h);
		current = new boolean[canvas.getWidth()][canvas.getHeight()];
		centerPair = new Pair(canvas.getWidth()/2, canvas.getHeight()/2);
	}

	public Starburst(Dimension size) {
		this(size.width, size.height);
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
			int thischar = myRandom.nextInt(62);
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
		/*boolean foundParams = false;
		for (PngChunk c : pngr.getChunksList().getChunks()) {
			if (!ChunkHelper.isText(c)) continue;
			PngChunkTextVar ct = (PngChunkTextVar) c;
			String key = ct.getKey();
			String val = ct.getVal();
			if(key.equalsIgnoreCase("Parameters")) {
				foundParams = true;
				System.out.printf("stored params: %s\n",val);
				String[] params = val.split("[,]");
				int i = 0;
				if(i < params.length) RBIAS = Double.parseDouble(params[i++].trim());
				if(i < params.length) GBIAS = Double.parseDouble(params[1].trim());
				if(i < params.length) BBIAS = Double.parseDouble(params[2].trim());
				if(i < params.length) CENTERBIAS = Double.parseDouble(params[3].trim())+1;
				if(i < params.length) GREYFACTOR = Integer.parseInt(params[4].trim());
				if(i < params.length) RANDOMFACTOR = Double.parseDouble(params[5].trim());
				if(i < params.length) SEED_METHOD = Integer.parseInt(params[6].trim());
				if(i < params.length) FINALIZATION_METHOD = Integer.parseInt(params[7].trim());
				RANDOMPROPERTIES = false;
				RANDOM_OTHER_PROPS = false;
			}
		}*/
		properties.importFromPNG(pngr.getChunksList().getChunks());
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
		properties.exportToPNG(meta);
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
		properties.randomize();
		System.out.println(properties);
		//loadPixels();
		pixels = new int[canvas.getWidth() * canvas.getHeight() * canvas.getRaster().getNumBands()];
		if(properties.getAsBoolean("renderDuringGeneration")) {
			savePixels();
		}
		falsifyCurrent();
		operations.setRemoveOrderBias(properties.getAsDouble("removeOrderBias"));
		seedImage(properties.getAsString("seedMethod"));
		System.out.println("done seeding");
		fillOperations();
		fillAll();
		System.out.println("done generating");
		savePixels();
		System.out.println("end newImage");
		System.out.printf("took %d millis\n",System.currentTimeMillis()-sTime);
	}

	private void savePixels() {
		canvas.getRaster().setPixels(0, 0, canvas.getWidth(), canvas.getHeight(), pixels);
		this.repaint();
	}

	private void loadPixels() {
		System.out.println(Runtime.getRuntime().freeMemory());
		if(Runtime.getRuntime().freeMemory() < canvas.getWidth()*canvas.getHeight() * 4) {
			System.out.print("Freeing Memory ... ");
			pixels = null;
			Runtime.getRuntime().gc();
			System.out.println(Runtime.getRuntime().freeMemory());
		}
		try {
			pixels = canvas.getRaster().getPixels(0, 0, canvas.getWidth(), canvas.getHeight(), (int[])null);
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
		g.drawImage(canvas, 0, 0, this);
	}

	private void setParams() {
		JComponent changeDialog = properties.getChangeDialog();
		final JDialog changeFrame = new JDialog();
		changeFrame.setModalityType(ModalityType.APPLICATION_MODAL);
		changeFrame.setContentPane(changeDialog);
		changeFrame.pack();
		//this.add(changeFrame, JLayeredPane.MODAL_LAYER);
		changeFrame.setLocation(this.getWidth()/2 - changeFrame.getWidth()/2, this.getHeight()/2 - changeFrame.getHeight()/2);
		changeFrame.setVisible(true);
		synchronized (changeDialog) {
			try {
				changeDialog.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		operations.setRemoveOrderBias(properties.getAsDouble("removeOrderBias"));
		if(properties.getAsInt("size.width") != canvas.getWidth() ||
		   properties.getAsInt("size.height") != canvas.getHeight()) {
			canvas = new BufferedImage(
					properties.getAsInt("size.width"), properties.getAsInt("size.height"), BufferedImage.TYPE_INT_RGB);
		}
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
		default:
			if(key != 27) asyncNewImage();
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

	private File chooseFile(int dialogType, int selectionMode) {
		final JInternalFrame fcFrame = new JInternalFrame();
		fcFrame.putClientProperty("JInternalFrame.frameType", "optionDialog");
		final boolean[] wasCanceled = new boolean[1];
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(selectionMode);
		fc.setDialogType(dialogType);
		fc.setMultiSelectionEnabled(false);
		fc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (JFileChooser.CANCEL_SELECTION.equals(cmd)) {
					fcFrame.setVisible(false);
					wasCanceled[0] = true;
					synchronized(fc) {
						fc.notifyAll();
					}
				} else if (JFileChooser.APPROVE_SELECTION.equals(cmd)) {
					fcFrame.setVisible(false);
					wasCanceled[0] = false;
					synchronized(fc) {
						fc.notifyAll();
					}
				}
			}
		});
		fcFrame.add(fc);
		fcFrame.pack();
		this.add(fcFrame, JLayeredPane.MODAL_LAYER);
		fcFrame.setLocation(this.getWidth()/2 - fcFrame.getWidth()/2, this.getHeight()/2 - fcFrame.getHeight()/2);
		fcFrame.setVisible(true);
		synchronized (fc) {
			try {
				fc.wait();
			} catch (InterruptedException e1) {
				return null;
			}
		}
		if(wasCanceled[0]) return null;
		return fc.getSelectedFile();
	}

	private void mousePressed() {
		System.out.println("mousePressed");
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
					Starburst.this.requestFocusInWindow();
					Starburst.this.requestFocus();
					if(VersionDependentMethodUtilities.appleEawtAvailable()) {
						VersionDependentMethodUtilities.appleForeground();
					}
				}
			}
		});
	}

	private int randomColor() {
		return myRandom.nextInt(0xffffff+1);
	}

	private void seedImage(String how) {
		if(how.equals("Points")) {
			for(int i = 0; i < properties.getAsInt("seedMethod.points.howMany"); i++) {
				Point p = randomRadialBiasedPoint(properties.getAsDouble("seedMethod.points.distribution"),
						canvas.getWidth(), canvas.getHeight());
				if(current[p.x][p.y]) continue;
				operations.addPoint(p.x, p.y);
				current[p.x][p.y] = true;
				setPixel(p.x, p.y, randomColor());
			}
		} else if(how.equals("Lines")) {
			int numberOfLines = (int)(properties.getAsDouble("seedMethod.lines.distribution.density") * 
								(canvas.getWidth()*canvas.getHeight()) /
								properties.getAsInt("seedMethod.lines.distribution.length"));
			if (numberOfLines<1) numberOfLines = 1;
			for (int j = 0; j < numberOfLines; j++) {
				generateLine(properties.getAsString("seedMethod.lines.colorScheme"));
			}
		} else {
			throw new IllegalArgumentException("Invalid seed method: "+how);
		}
	}

	private Point randomRadialBiasedPoint(double bias, int width, int height) {
		Point p = new Point(-1, -1); // intentionally invalid
		while(p.x < 0 || p.y < 0 || p.x >= width || p.y >= height) { // keep trying until valid
			double angle = myRandom.nextDouble() * 2*PI;
			double radiasBais = bias==0? 0: pow(myRandom.nextDouble(), log(bias)/log(.5));
			double radius = radiasBais * sqrt(width*width + height*height)/2; // maximum possible, may not be valid
			p = new Point((int)(radius*cos(angle)) + width/2, (int)(radius*sin(angle)) + height/2);
		}
		return p;
	}

	private void generateLine(String colorScheme) {
		int c;
		if(colorScheme.equalsIgnoreCase("Varying")) {
			c = randomColor();
		} else if(colorScheme.equalsIgnoreCase("Solid")) {
			c = properties.get(Color.class, "seedMethod.lines.colorScheme.solid.color").getRGB();
		} else {
			throw new IllegalStateException(colorScheme+" is not a known colorScheme to generate a line");
		}
		double x = myRandom.nextInt(canvas.getWidth()), y = myRandom.nextInt(canvas.getHeight());
		double r = myRandom.nextInt(255), g = myRandom.nextInt(255), b = myRandom.nextInt(255);
		double rx = myRandom.nextInt(7)-3, ry = myRandom.nextInt(7)-3;
		double rr = myRandom.nextInt(7)-3, rg = myRandom.nextInt(7)-3, rb = myRandom.nextInt(7)-3;
		for (int i = 0; i < properties.getAsInt("seedMethod.lines.distribution.length"); i++) {
			rx = lerp(rx, myRandom.nextInt(7)-3, .05);
			ry = lerp(ry, myRandom.nextInt(7)-3, .05);
			rr = lerp(rr, myRandom.nextInt(7)-3, .05);
			rg = lerp(rg, myRandom.nextInt(7)-3, .05);
			rb = lerp(rb, myRandom.nextInt(7)-3, .05);

			double d = hypot(rx, ry);
			double cd = sqrt(rr*rr + rg*rg + rb*rb);

			rx=rx/d; ry=ry/d;
			rr=rr/cd; rg=rg/cd; rb=rb/cd;

			x = x+rx; y = y+ry;
			r = r+rx; g = g+rx; b = b+ry;

			if (x<0) {
				x=0;
				rx=-rx;
			}
			if (y<0) {
				y=0;
				ry=-ry;
			}
			if (x>=canvas.getWidth()) {
				x=canvas.getWidth()-1;
				rx=-rx;
			}
			if (y>=canvas.getHeight()) {
				y=canvas.getHeight()-1;
				ry=-ry;
			}
			if (r<0) {
				r=0;
				rr=-rr;
			}
			if (g<0) {
				g=0;
				rg=-rg;
			}
			if (b<0) {
				b=0;
				rb=-rb;
			}
			if (r>=255) {
				r=255-1;
				rr=-rr;
			}
			if (g>=255) {
				g=255-1;
				rg=-rg;
			}
			if (b>=255) {
				b=255-1;
				rb=-rb;
			}
			if (colorScheme.equals("Varying")) c = color((int)r,(int)g,(int)b);
			current[(int)x][(int)y] = true;
			setPixel((int)x, (int)y, c);
		}
	}

	private void fillOperations() {
		for (int x = 0; x < current.length; x++) {
			for (int y = 0; y < current[0].length; y++) {
				if (current[x][y]) {
					for(Pair p: getNeighbors(x, y)) {
						if(!current[p.x][p.y]) operations.addPoint(x, y); 
					}
					if (x+1<current.length && !current[x+1][y]) operations.addPoint(x+1, y);
					if (x-1>=0 && !current[x-1][y]) operations.addPoint(x-1, y);
					if (y+1<current[0].length && !current[x][y+1]) operations.addPoint(x, y+1);
					if (y-1>=0 && !current[x][y-1]) operations.addPoint(x, y-1);
				}
			}
		}
	}

	private int getPixel(int x, int y) {
		int i = (x+(y*canvas.getWidth()))*3;
		return color(pixels[i], pixels[i+1], pixels[i+2]);
	}

	private void setPixel(int x, int y, int c) {
		int i = (x+(y*canvas.getWidth()))*3;
		pixels[i]=red(c);
		pixels[i+1]=green(c);
		pixels[i+2]=blue(c);
		if(properties.getAsBoolean("renderDuringGeneration")) {
			canvas.setRGB((int)x, (int)y, c);
			this.repaint((int)x, (int)y, 1, 1);
		}
	}

	private static double lerp(double a, double b, double lerpVal){
		return a + b*lerpVal;
	}

	private void falsifyCurrent() {
		for (int i=0;i<canvas.getWidth();i++) {
			for (int j=0;j<canvas.getHeight();j++) {
				current[i][j]=false;
			}
		}
	}

	private void fillAllPixels() {
		while (operations.hasPoint()) {
			Pair myPair = operations.getPoint();
			if (myPair == null) continue;
			int x = myPair.x, y = myPair.y;
			if (current[x][y]) continue;
			fillPixel(x, y);
			for(Pair p: getNeighbors(x, y)) {
				if(!current[p.x][p.y]){
					if(myRandom.nextDouble() < properties.getAsDouble("probabilityOfInclusion")) {
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
		return neighbors;
	}

	private void fillAll() {
		List<Callable<Object>> threads = new ArrayList<Callable<Object>>();
		for (int i=0;i<THREADNUM;i++) {
			threads.add(new Callable<Object>() {
				public Object call() throws Exception {
					try{
						fillAllPixels();
					} catch(Exception e) {
						e.printStackTrace();
						// would be ignored if I just passed it
						// at least this way I hear about it
						throw e;
					}
					return null;
				}
			});
		}
		try {
			exec.invokeAll(threads);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if(properties.getAsDouble("probabilityOfInclusion") < 1) {
			finalizePixels(properties.getAsString("finalizationMethod"));
		}
		operations.clear();
	}

	private void randomSeedPixels() {
		for (int x=0;x<canvas.getWidth();x++) {
			for (int y=0;y<canvas.getHeight();y++) {
				if (!current[x][y]&&myRandom.nextInt(1000)<2) {
					for (int i=x;i<x+10&&i<canvas.getWidth();i++) {
						for (int j=y;j<y+10&&j<canvas.getHeight();j++) {
							if (!current[i][j]) fillPixel(i, j);
						}
					}
				}
			}
		}
	}

	private void finalizePixels(String how) {
		if(how.equals("Squares then loop (x,y)")) {
			randomSeedPixels();
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		} else if(how.equals("Loop (x,y)")) {
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		} else if(how.equals("Fill with black")) {
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) setPixel(x, y, Color.BLACK.getRGB());
				}
			}
		} else if(how.equals("Redo from point")) {
			boolean[][] localcurrent = new boolean[canvas.getWidth()][canvas.getHeight()];
			operations.addPoint(centerPair.x,centerPair.y);
			Pair myPair;
			while ((myPair = operations.getPoint()) != null) {
				int x=myPair.x, y=myPair.y;
				if (localcurrent[x][y]) continue;
				if (!current[x][y]) fillPixel(x, y);
				if (((y+1)<canvas.getHeight())&&!localcurrent[x][y+1]) {
					operations.addPoint(x, y+1);
				}
				if (((x+1)<canvas.getWidth())&&!localcurrent[x+1][y]) {
					operations.addPoint(x+1, y);
				}
				if (((y-1)>=0)&&!localcurrent[x][y-1]) {
					operations.addPoint(x, y-1);
				}
				if (((x-1)>=0)&&!localcurrent[x-1][y]) {
					operations.addPoint(x-1, y);
				}
				localcurrent[x][y]=true;
			}
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
	private void fillPixel(int x, int y) {
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
			setPixel(x, y, randomColor());
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

	private static int clamp(int min, int x, int max) {
		return (x <= min)? min: (x >= max)? max: x;
	}

	private static int color(int r, int g, int b){
		return (r << 16) | (g << 8) | b;
	}

	private static int red(int rgb){
		return (rgb & 0x00ff0000) >> 16;
	}

	private static int green(int rgb){
		return (rgb & 0x0000ff00) >> 8;
	}

	private static int blue(int rgb){
		return rgb & 0x000000ff;
	}

	int biasedRandom(int minVal, int maxVal, double biastocenter, double biasFactor) {

		if (maxVal<minVal) {
			if (properties.getAsString("oorResolution").equals("Randomly choose one side")) {
				return myRandom.nextBoolean()? minVal: maxVal;
			} else {
				return ((maxVal+minVal)/2);
			}
		}

		double a = .5 + (myRandom.nextBoolean()? -.5: .5)*pow(myRandom.nextDouble(), biastocenter);
		double val = (a*(double)(maxVal-minVal+biasFactor+1))+minVal;
		return (int)val;
	}

}
