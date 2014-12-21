package net.clonecomputers.lab.starburst;

import static java.lang.Math.*;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;

import net.clonecomputers.lab.starburst.properties.Properties;
import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;

import com.madgag.gif.fmsware.*;

public class Starburst extends JDesktopPane {
	//final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
	private Random myRandom = new Random();
	private BufferedImage canvas;
	
	private Properties properties = new Properties();
//
//	private int negvar = 15;
//	private int posvar = 15;
//
//	/**
//	 * How any missing pixels are filled in <br /><br />
//	 * -1 > random <br />
//	 * 0 -> squares then loop x,y <br />
//	 * 1 -> loop x,y <br />
//	 * 2 -> fill with black <br />
//	 * 3 -> run almost normal program from center <br />
//	 * 4 -> do nothing
//	 */
//	private int FINALIZATION_METHOD = 3;
//
//	/**
//	 * How the image is seeded <br /><br />
//	 * -1 > random <br />
//	 * 0 -> 13 random points <br />
//	 * 1 -> black lines <br />
//	 * 2 -> colored lines <br />
//	 * 3 -> center point <br />
//	 */
//	private int SEED_METHOD = 2;
//
//	//boolean RANDOMFACTOR<0 = true;
//
//	//private int REMOVE_ORDER = 0;
//	//-1 > random
//	//0 -> random
//	//1 -> first
//
//	private boolean GEN_ONLY_ONE_THEN_SAVE_AND_EXIT_MODE = false;
//
//	private String SAVE_DIRECTORY = "/usr/share/images/clone-desktop/starburst-backgrounds/";
//
//	/**
//	 * randomize image properties each time w/in a list of good values
//	 */
//	private boolean RANDOMPROPERTIES = true;
//
//	/**
//	 * randomize seed and finalization properties each time
//	 * w/in a list of good values
//	 */
//	private boolean RANDOM_OTHER_PROPS = true;
//
//	/**
//	 * The length of a line.  <br />
//	 * 1000 usually gives a good length, I think.
//	 * there is a bit of turning and doubling back, 
//	 * so the lines appear somewhat much shorter
//	 */
//	private int LINE_LENGTH = 1000;
//
//	/**
//	 * This is used to calculate how many lines to generate.  <br />
//	 * number of lines = (w*h)/(LINE_LENGTH*AVERAGE_INVERSE_LINE_DENSITY)
//	 */
//	private int AVERAGE_INVERSE_LINE_DENSITY = 300;
//
//	/**
//	 * whether to average when no possible values are found
//	 * or to choose a random endpoint
//	 */
//	private boolean SHARP = true;
//
//	/**
//	 * The bias on the red component.  
//	 * 0 is no bias.  Higher numbers for lighter,
//	 * lower numbers for darker
//	 */
//	private double RBIAS = 0;
//
//	/**
//	 * The bias on the blue component.  
//	 * 0 is no bias.  Higher numbers for lighter,
//	 * lower numbers for darker
//	 */
//	private double GBIAS = 0;
//
//	/**
//	 * The bias on the blue component.  
//	 * 0 is no bias.  Higher numbers for lighter,
//	 * lower numbers for darker
//	 */
//	private double BBIAS = 0;
//
//	/**
//	 * How much color components are biased towards the average of their neighbors.
//	 * 1 is no bias, higher means more towards center.  
//	 * Bigger numbers make the image tend to have larger patches of similar colors together
//	 */
//	private double CENTERBIAS = 10;
//
//	/**
//	 * This can be used to make the image in general greyer.  
//	 * The maximum value of any color component is 255&nbsp;-&nbsp;GREYFACTOR.  
//	 * the minimum value of any color component is GREYFACTOR.  
//	 * 0 is no bias.  Bigger numbers are greyer, up to 127
//	 * to make the image entirely grey
//	 */
//	private int GREYFACTOR = 0;
//
//	/**
//	 * This is used to determine if a cell should be included.  
//	 * the probabiity that a cell will be included on one check of this is
//	 * RANDOMFACTOR&nbsp;/&nbsp;(RANDOMFACTOR&nbsp;+&nbsp;1).  
//	 * 0 is 100% chance
//	 */
//	private double RANDOMFACTOR = 1.05;
//
//	private int THREADNUM = 5;

	private AnimatedGifEncoder gifEnc = null;

	private static final JLabel[] PARAM_CHANGE_MESSAGE = {
		new JLabel("Please input the image generation paramaters in the form"),
		new JLabel("(red bias, green bias, blue bias, center bias, grey factor, random layout factor)"),
		new JLabel("or 'random' for random values or 'same' for current values, or cancel to exit unchanged"),
	};
	private static final JLabel[] OTHER_PARAM_CHANGE_MESSAGE = {
		new JLabel("Please input the seeding and finalizations paramaters in the form"),
		new JLabel("(seed method, remove order, out of range resolution, finalization method)"),
		new JLabel("or 'random' for random values or 'same' for current values, or cancel to exit unchanged"),
		new JLabel("# |  Seed Method  | Remove order | Out of Range  | Finalization"),
		new JLabel("---------------------------------------------------------------"),
		new JLabel("0 |   13 points   |    random    |    average    | squares then loop x,y"),
		new JLabel("1 |  black lines  |    first     | pick one side | loop x,y" + "\n"),
		new JLabel("2 | colored lines |    last      |               | fill with black"),
		new JLabel("3 | center point  |              |               | run normally from center point"),
	};
	static {
		for(JLabel l: PARAM_CHANGE_MESSAGE) l.setFont(new Font("Monospaced",Font.PLAIN,12));
		for(JLabel l: OTHER_PARAM_CHANGE_MESSAGE) l.setFont(new Font("Monospaced",Font.PLAIN,12));
	}
	private boolean current[][];
	public static final ExecutorService exec = Executors.newCachedThreadPool();
	private static final int THREADNUM = 2;
	//Executors.newFixedThreadPool(THREADNUM+1); // +1 for system.in listener
	private Pair centerPair;
	private int[] pixels;
	private PixelOperationsList operations;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable(){
			private boolean makingGif;

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
				//window.setBackground(Color.WHITE);
				//window.setForeground(Color.WHITE);
				Dimension size;
				AnimatedGifEncoder gifEnc = null;
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
				if(Arrays.binarySearch(args,"gif") > 0) {
					FileDialog gifFileFinder = new FileDialog((Frame)null, "Where do you want to save the gif", FileDialog.SAVE);
					gifFileFinder.setVisible(true);
					while(gifFileFinder.getFile() == null) Thread.yield();
					File f = new File(gifFileFinder.getDirectory(), gifFileFinder.getFile());
					try {
						f.createNewFile();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					gifEnc = new AnimatedGifEncoder();
					gifEnc.setDispose(1);
					gifEnc.setBackground(Color.WHITE);
					gifEnc.setTransparent(Color.WHITE);
					try {
						gifEnc.start(new BufferedOutputStream(new FileOutputStream(f)));
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
					size = new Dimension(
							Integer.parseInt(args[1].trim()),
							Integer.parseInt(args[2].trim()));
					BufferedImage allBlack = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					Graphics g = allBlack.getGraphics();
					g.drawRect(0, 0, allBlack.getWidth(), allBlack.getHeight());
					gifEnc.addFrame(allBlack);
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
				Starburst s = new Starburst(size, gifEnc);
				s.setupKeyAndClickListeners(window);
				window.setContentPane(s);
				if(gifEnc == null) {
					VersionDependentMethodUtilities.enableFullscreen(window,biggestScreen,false);
				} else {
					window.setVisible(true);
					window.setSize(size);
				}
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
			/*@Override public void keyReleased(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER) mousePressed();
				//if(e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
			}*/
			@Override public void keyTyped(KeyEvent e){
				Starburst.this.keyPressed(e.getKeyChar());
			}
		});
		this.addKeyListener(new KeyAdapter(){
			/*@Override public void keyReleased(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER) mousePressed();
				//if(e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
			}*/
			@Override public void keyTyped(KeyEvent e){
				Starburst.this.keyPressed(e.getKeyChar());
			}
		});
	}

	public Starburst(int w,int h){
		//fc.setUI(new )
		/*try{
	   javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	   }catch(Exception e){
	   throw new RuntimeException(e);
	   }*/
		//pixels = new int[w*h];
		canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		this.setPreferredSize(new Dimension(w,h));
		operations = new PixelOperationsList(w*h);
		properties = new Properties();
		properties.set("size.width", w);
		properties.set("size.height",h);
		//background(randnum(256), randnum(256), randnum(256));
		//background(0);
		current = new boolean[canvas.getWidth()][canvas.getHeight()];
		centerPair = new Pair(canvas.getWidth()/2, canvas.getHeight()/2);
		//newImage();
		/*if (GEN_ONLY_ONE_THEN_SAVE_AND_EXIT_MODE) {
			saveRandomName(SAVE_DIRECTORY);
			System.exit(0);
		}*/
	}

	public Starburst(Dimension size) {
		this(size.width, size.height);
	}

	public Starburst(Dimension size, AnimatedGifEncoder gifEnc) {
		this(size.width, size.height);
		this.gifEnc = gifEnc;
	}

	private void genMany(String outputDirectory, int howMany) {
		for (int i=0;i<howMany;i++) {
			newImage();
			saveRandomName(outputDirectory);
		}
		System.exit(0);
	}

	private void saveRandomName(String outputDirectory) {
		String filename = String.format("%s/%s %s.png",
				outputDirectory,
				properties.toString().replace('\n', ' ').replace('.','_'),
				randomstr(8));
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
		//if(!foundParams) System.err.println("failed to find params");
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
		/*int[] buf = new int[canvas.getWidth()*3]; // one row
		for(int i = 0; i < pixels.length; i++) {
			buf[(i*3)%buf.length] = (pixels[i] & 0xFF0000)>>16; // R
			buf[(i*3+1)%buf.length] = (pixels[i] & 0xFF00)>>8;  // G
			buf[(i*3+2)%buf.length] = (pixels[i] & 0xFF);       // B
			if((i*3)%buf.length == 0 && i>0) writer.writeRowInt(buf);
		}
		writer.writeRowInt(buf);*/
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
		/*ImageWriter i;
		Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(suffix);
		if(writers.hasNext()) {
			i = writers.next();
		} else {
			f = new File(filename+".png");
			i = ImageIO.getImageWritersBySuffix("png").next();
		}
		try {
			i.setOutput(new FileImageOutputStream(f));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			i.write(canvas);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
	}
/*
	private void randomizeProperties() {
		randomizeRBias();
		randomizeGBias();
		randomizeBBias();
		randomizeCenterBias();
		randomizeRandomfactor();
	}

	private void randomizeOtherProperties() {
		randomizeSeedMethod();
		randomizeFinalization();
		randomizeRemoveOrder();
		randomizeSharp();
	}

	private void randomizeRBias() { RBIAS = randomListDouble(randomRange(-.5, .5), randomRange(-.5, .5), randomRange(-1.5, 1.5)); }
	private void randomizeGBias() { GBIAS = randomListDouble(randomRange(-.5, .5), randomRange(-.5, .5), randomRange(-1.5, 1.5)); }
	private void randomizeBBias() { BBIAS = randomListDouble(randomRange(-.5, .5), randomRange(-.5, .5), randomRange(-1.5, 1.5)); }
	private void randomizeCenterBias() { CENTERBIAS = randomListDouble(randomRange(0, 15), randomRange(10, 15), 0)+1; }
	private void randomizeRandomfactor() { RANDOMFACTOR = randomListDouble(randomRange(0, 1), randomRange(0, 1), randomRange(1, 2), randomRange(0, 3), 20, 100); }

	private void randomizeSeedMethod() { SEED_METHOD = randomListInt(0, 1, 2, 3); }
	private void randomizeFinalization() { FINALIZATION_METHOD = randomListInt(0, 1, 2, 3); }
	private void randomizeRemoveOrder() { operations.setRemoveOrder(randomListInt(0,0,0,1,2,2)); }
	private void randomizeSharp() { SHARP = myRandom.nextBoolean(); }

	private double randomRange(double min, double max) {
		return (myRandom.nextDouble() * (max-min)) + min;
	}

	private int randomListInt(int... list) {
		return list[myRandom.nextInt(list.length)];
	}

	private double randomListDouble(double... list) {
		return list[myRandom.nextInt(list.length)];
	}
*/
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
		System.out.println(properties);
		/*
		System.out.println(String.format("%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d, %d, %b", 
				RBIAS, GBIAS, BBIAS, CENTERBIAS, 
				GREYFACTOR, RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD, operations.getRemoveOrder().i(), SHARP));
		if (RANDOMPROPERTIES) randomizeProperties();
		if (RANDOM_OTHER_PROPS) randomizeOtherProperties();

		//if(RBIAS < 0) randomizeRBias();
		//if(GBIAS < 0) randomizeGBias();
		//if(BBIAS < 0) randomizeBBias();
		//if(CENTERBIAS < 0) randomizeCenterBias();
		if(RANDOMFACTOR < 0) randomizeRandomfactor();
		 
		if(SEED_METHOD < 0) randomizeSeedMethod();
		if(FINALIZATION_METHOD < 0) randomizeFinalization();
		//if(operations.getRemoveOrder().i() < 0) randomizeRemoveOrder();
		*/
		
		pixels = new int[canvas.getWidth()*canvas.getHeight()*canvas.getColorModel().getNumComponents()];
		savePixels();
		//loadPixels();
		falsifyCurrent();
		seedImage(properties.getAsString("seedMethod"));
		if(gifEnc != null) {
			gifEnc.setDelay(500);
		}
		System.out.println("done seeding");
		fillOperations();
		fillAll();
		System.out.println("done generating");
		//loop();
		savePixels();
		//cancelPhantomProcessorUsage();
		System.out.println("end newImage");
		System.out.printf("took %d millis\n",System.currentTimeMillis()-sTime);
		if(gifEnc != null) {
			gifEnc.finish();
			System.exit(0);
		}
	}

	private void savePixels() {
		//((DataBufferInt) canvas.getData().getDataBuffer()).
		canvas.getRaster().setPixels(0, 0, canvas.getWidth(), canvas.getHeight(), pixels);
		/*int i = 0;
		for(int y = 0; y < canvas.getHeight(); y++){
			for(int x = 0; x < canvas.getWidth(); x++){
				canvas.setRGB(x, y, pixels[i++]);
			}
		}*/
		//this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
		this.repaint();
	}

	private void loadPixels() {
		pixels = canvas.getRaster().getPixels(0, 0, canvas.getWidth(), canvas.getHeight(), (int[])null);
		//pixels = ((DataBufferInt) canvas.getData().getDataBuffer()).getData();
	}

	@Override public void paintComponent(Graphics g){
		super.paintComponent(g);
		g.drawImage(canvas, 0, 0, this);
	}
/*
	private void setOtherParams() {
		String curprops = String.format("%d, %d, %d, %d", 
				SEED_METHOD, operations.getRemoveOrder().i(), SHARP?1:0, FINALIZATION_METHOD);
		//System.out.println(curprops);
		Object in = javax.swing.JOptionPane.showInternalInputDialog(this,OTHER_PARAM_CHANGE_MESSAGE,
				"Set Parameters", JOptionPane.QUESTION_MESSAGE, null, null, curprops);
		if(in == null) return;
		String input = in.toString();
		if (input==null) return;
		if (input.equalsIgnoreCase("random")) {
			RANDOM_OTHER_PROPS = true;
			//newImage();
			return;
		}
		if (input.equalsIgnoreCase("same")) {
			RANDOM_OTHER_PROPS = false;
			return;
		}
		if (input.charAt(0)=='(') input = input.substring(1, input.length()-1);
		String[] params = input.split(",");
		try {
			SEED_METHOD = Integer.parseInt(params[0].trim());
			operations.setRemoveOrder(Integer.parseInt(params[1].trim()));
			SHARP = Integer.parseInt(params[2].trim())>0;
			FINALIZATION_METHOD = Integer.parseInt(params[3].trim());
		}
		catch(Exception e) {
			System.out.println(e+" in setParams()");
		}
		RANDOM_OTHER_PROPS = false;
		//newImage();
	}

	private void setParams() {
		String curprops = String.format("%.2f, %.2f, %.2f, %.2f, %d, %.2f", 
				RBIAS, GBIAS, BBIAS, CENTERBIAS, 
				GREYFACTOR, RANDOMFACTOR);
		Object in = javax.swing.JOptionPane.showInternalInputDialog(this,PARAM_CHANGE_MESSAGE,
				"Set Parameters", JOptionPane.QUESTION_MESSAGE, null, null, curprops);
		if(in == null) return;
		String input = in.toString();
		if(input==null) return;
		if(input.equalsIgnoreCase("random")) {
			RANDOMPROPERTIES = true;
			//newImage();
			return;
		}
		if(input.equalsIgnoreCase("same")) {
			RANDOMPROPERTIES = false;
			return;
		}
		if(input.charAt(0)=='(') input = input.substring(1, input.length()-1);
		String[] params = input.split(",");
		try {
			RBIAS = Double.parseDouble(params[0].trim());
			GBIAS = Double.parseDouble(params[1].trim());
			BBIAS = Double.parseDouble(params[2].trim());
			CENTERBIAS = Double.parseDouble(params[3].trim());
			GREYFACTOR = Integer.parseInt(params[4].trim());
			RANDOMFACTOR = Double.parseDouble(params[5].trim());
		}
		catch(Exception e) {
			System.out.println(e+" in setParams()");
		}
		RANDOMPROPERTIES = false;
		//newImage();
	}
*/
	
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
					//System.out.print("copying variables from ");
					File input = chooseFile(JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
					if(input == null) {
						System.out.println("cancled");
						return;
					}
					//System.out.println(input);
					copyParamsFromFile(input);
					//System.out.println("done");
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
		//String savePath = selectOutput("select an output file");  // Opens file chooser
		/*fc.showSaveDialog(this);
		if (fc.getSelectedFile()==null) return;
		String savePath = fc.getSelectedFile().getAbsolutePath();*/
		exec.execute(new Runnable(){@Override public void run(){
			File output = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
			//Starburst.this.requestFocus();
			if(output == null) {
				// If a file was not selected
				System.out.println("No output file was selected...");
			} 
			else {
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
		}});
	}

	private int randomColor() {
		return myRandom.nextInt(0xffffff+1);
		//new Color(myRandom.nextFloat(),myRandom.nextFloat(),myRandom.nextFloat()).getRGB();
	}

	private void seedImage(String how) {
		if(how.equals("Points")) {
			//addPoint(centerPair)
			for (int i = 0; i < 13; i++) {
				int x = myRandom.nextInt(canvas.getWidth()), y =  myRandom.nextInt(canvas.getHeight());
				operations.addPoint(x,y);
				current[x][y] = true;
				setPixel(x, y, randomColor());
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
		//updatePixels();//I/NFO: updatePixels() for show
	}

	private void generateLine(String colorScheme) {
		int c = colorScheme.equals("Varying")? 
				randomColor(): properties.get(Color.class, "seedMethod.lines.colorScheme.solid.color").getRGB();
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
			//c = lerpColor(c,(int)randnum(#FFFFFF),.1);
			//operations.add(new Pair((int)x, (int)y));
			current[(int)x][(int)y] = true;
			setPixel((int)x, (int)y, c);
			if(colorScheme.equals("Varying")) {
				canvas.setRGB((int)x, (int)y, c);
				this.repaint((int)x, (int)y, 1, 1);
			}
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
		canvas.setRGB((int)x, (int)y, c);
		this.repaint((int)x, (int)y, 1, 1);
		if(gifEnc != null) {
			BufferedImage frameImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = frameImage.getGraphics();
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, canvas.getWidth(), canvas.getHeight());
			g.setColor(new Color(c));
			g.drawRect(x, y, 1, 1);
			gifEnc.setDelay(0);
			gifEnc.addFrame(frameImage);
			gifEnc.setTransparent(Color.BLACK);
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
		/*exec.execute(new Runnable() {
			@Override
			public void run() {
				while(doneCount < THREADNUM) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					updatePixels(); //I/NFO: updatePixels() for show
				}
			}
		});*/
		List<Callable<Object>> threads = new ArrayList<Callable<Object>>();
		for (int i=0;i<THREADNUM;i++) {
			threads.add(new Callable<Object>() {
				public Object call() {
					//System.out.println("running");
					try{
						fillAllPixels();
					} catch(Throwable t) {
						t.printStackTrace();
						// would be ignored if I just passed it
						// at least this way I hear about it
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
	}

	private void randomSeedPixels() {
		for (int x=0;x<canvas.getWidth();x++) {
			for (int y=0;y<canvas.getHeight();y++) {
				if (!current[x][y]&&myRandom.nextInt(1000)<2) {
					for (int i=x;i<x+10&&i<canvas.getWidth();i++) {
						for (int j=y;j<y+10&&j<canvas.getHeight();j++) {
							if (!current[i][j]) fillPixel(i, j);
							//println("("+i+","+j+")");
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
		int maxr=255, minr=0, maxg=255, ming=0, maxb=255, minb=0;
		int neighborColors[]=new int[4];
		int neighborsFullYet=0;
		if (((y+1)<canvas.getHeight())&&current[x][y+1]) {
			neighborColors[neighborsFullYet++] = getPixel(x, y+1);
		}
		if (((x+1)<canvas.getWidth())&&current[x+1][y]) {
			neighborColors[neighborsFullYet++] = getPixel(x+1, y);
		}
		if (((y-1)>=0)&&current[x][y-1]) {
			neighborColors[neighborsFullYet++] = getPixel(x, y-1);
		}
		if (((x-1)>=0)&&current[x-1][y]) {
			neighborColors[neighborsFullYet++] = getPixel(x-1, y);
		}
		if(neighborsFullYet == 0) {
			setPixel(x, y, randomColor());
			current[x][y] = true;
			return;
		}
		for (int i=0;i<neighborsFullYet;i++) {
			int curCol = neighborColors[i];
			int curr=red(curCol), curg=green(curCol), curb=blue(curCol);
			if (maxr>curr) maxr=(curr+properties.getAsInt("pixelVariation.positiveVariation"));
			if (minr<curr) minr=(curr-properties.getAsInt("pixelVariation.negativeVariation"));
			if (maxg>curg) maxg=(curg+properties.getAsInt("pixelVariation.positiveVariation"));
			if (ming<curg) ming=(curg-properties.getAsInt("pixelVariation.negativeVariation"));
			if (maxb>curb) maxb=(curb+properties.getAsInt("pixelVariation.positiveVariation"));
			if (minb<curb) minb=(curb-properties.getAsInt("pixelVariation.negativeVariation"));
		}
		/*
		maxr = clamp(0,maxr,255);
		minr = clamp(0,minr,255);
		maxg = clamp(0,maxg,255);
		ming = clamp(0,ming,255);
		maxb = clamp(0,maxb,255);
		minb = clamp(0,minb,255);
		 */
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
		return (r << 16) + (g << 8) + b;
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

		double a = myRandom.nextDouble();
		a = .5 + (myRandom.nextBoolean()? -.5: .5)*pow(a, biastocenter);
		//System.out.println(a);
		double val = (a*(double)(maxVal-minVal+biasFactor+1))+minVal;
		return (int)val;
		/*
		//if (framenum!=0) return (int)randnum(maxVal-minVal+1)+minVal;
		double n = 0F;
		for (int i=0;i<biastocenter;i++) {
			n+=(myRandom.nextDouble()*((double)(maxVal-minVal+biasFactor+1)) + minVal) / biastocenter;
		}
		return (int) n;
		//return (int) (randnum((double)(maxVal-minVal+biasFactor+1))+minVal);
		 */
	}

}
