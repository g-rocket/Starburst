package net.clonecomputers.lab.starburst;

import static java.lang.Math.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;

public class Starburst extends JDesktopPane {
	//final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
	private Random myRandom = new Random();
	private BufferedImage canvas;

	private int negvar = 15;
	private int posvar = 15;

	private boolean looping = false;

	private int doneCount=0;

	private int FINALIZATION_METHOD = 3;
	//-1 > random
	//0 -> squares then loop x,y
	//1 -> loop x,y
	//2 -> fill with black
	//3 -> run almost normal program from center
	//4 -> do nothing

	private int SEED_METHOD = 2;
	//-1 > random
	//0 -> 13 random points
	//1 -> black lines
	//2 -> colored lines
	//3 -> center point

	//boolean RANDOMFACTOR<0 = true;

	//private int REMOVE_ORDER = 0;
	//-1 > random
	//0 -> random
	//1 -> first

	private boolean GEN_ONLY_ONE_THEN_SAVE_AND_EXIT_MODE = false;

	private String SAVE_DIRECTORY = "/usr/share/images/clone-desktop/starburst-backgrounds/";

	private boolean RANDOMPROPERTIES = true; // randomize image properties
	//each time w/in a list of good values

	private boolean RANDOM_OTHER_PROPS = true; // randomize seed and
	//finalization properties each time w/in a list of good values

	private int LINE_LENGTH = 1000;// length of a line
	// 10000 gives a good length, usually, I think
	// there is a lot of turning and doubling back, 
	// so the lines are actually much shorter
	private int AVERAGE_INVERSE_LINE_DENSITY = 300;
	// number of lines = (w*h)/(LINE_LENGTH*AVERAGE_INVERSE_LINE_DENSITY)

	private boolean SHARP = true; //whether to average when 
	//no possible values are found or to choose a random endpoint

	private double RBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker
	private double GBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker
	private double BBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker

	private double CENTERBIAS = 10; //1 is no bias, higher means more towards center
	// bigger numbers also take longer to make an image, but mean more toned down

	private int GREYFACTOR = 0;//0 is no bias.  
	// bigger numbers for greyer, up to 127 for all grey

	private double RANDOMFACTOR = 1.05;
	// the probabiity check that determines if a cell should be included
	// return true RANDOMFACTOR / (RANDOMFACTOR + 1) of the time
	// 0 is 100% chance

	private int genNum = 30;

	private int THREADNUM = 5;

	private static final String PARAM_CHANGE_MESSAGE = 
			"Please input the image generation paramaters in the form" + "\n" +
			"(red bias, green bias, blue bias, center bias, grey factor, random layout factor)" + "\n" +
			"or 'random' for random values or 'same' for current values, or cancel to exit unchanged";
	private static final String OTHER_PARAM_CHANGE_MESSAGE = 
			"Please input the seeding and finalizations paramaters in the form" + "\n" +
			"(seed method, finalization method)" + "\n" +
			"or 'random' for random values or 'same' for current values, or cancel to exit unchanged" + "\n" +
			"# |  Seed Method  | Remove order | Out of Range  | Finalization" + "\n" +
			"---------------------------------------------------------------" + "\n" +
			"0 |   13 points   |    random    |    average    | squares then loop x,y" + "\n" +
			"1 |  black lines  |    first     | pick one side | loop x,y" + "\n" +
			"2 | colored lines |    last      |               | fill with black" + "\n" +
			"3 | center point  |              |               | run normally from center point" + "\n";
	private int pixnum=0;
	private boolean current[][];
	public static final ExecutorService exec = Executors.newCachedThreadPool();
	//Executors.newFixedThreadPool(THREADNUM+1); // +1 for system.in listener
	private Pair centerPair;
	private int[] pixels;
	private PixelOperationsList operations;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable(){@Override public void run(){
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
			switch(args.length) {
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
			default: // args.length > 2 means read from stdin
				System.out.println("Input dimensions");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String arg0;
				try {
					arg0 = reader.readLine().trim();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if(arg0.matches("\\d+\\s*,\\s*\\d+")){ // WWW, HHH
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
			VersionDependentMethodUtilities.toFullScreen(window,biggestScreen);
			s.asyncNewImage();
		}});
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
						String arg = r.readLine().trim();
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
		//background(randnum(256), randnum(256), randnum(256));
		//background(0);
		current = new boolean[canvas.getWidth()][canvas.getHeight()];
		centerPair = new Pair(canvas.getWidth()/2, canvas.getHeight()/2);
		//newImage();
		if (GEN_ONLY_ONE_THEN_SAVE_AND_EXIT_MODE) {
			saveRandomName(SAVE_DIRECTORY);
			System.exit(0);
		}
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
		String filename = String.format("%s/%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d %s.png",
				outputDirectory,
				RBIAS, GBIAS, BBIAS, CENTERBIAS-1, GREYFACTOR,
				RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD, randomstr(8));
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
		boolean foundParams = false;
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
		}
		pngr.end(); // not necessary here, but good practice
		if(!foundParams) System.err.println("failed to find params");
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
		meta.setText("Parameters",String.format("%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d, %d, %b",
				RBIAS, GBIAS, BBIAS, CENTERBIAS-1, GREYFACTOR,
				RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD, operations.getRemoveOrder().i(), SHARP));

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

		pixels = new int[canvas.getWidth()*canvas.getHeight()*canvas.getColorModel().getNumComponents()];
		savePixels();
		//loadPixels();
		falsifyCurrent();
		seedImage(SEED_METHOD);
		System.out.println("done seeding");
		fillOperations();
		fillAll();
		System.out.println("done generating");
		//loop();
		savePixels();
		//cancelPhantomProcessorUsage();
		System.out.println("end newImage");
		System.out.printf("took %d millis\n",System.currentTimeMillis()-sTime);
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

	private void keyPressed(char key) {
		key = String.valueOf(key).toLowerCase().charAt(0);
		switch(key) {
		case 'v':
			mousePressed();
		break;
		case 'p':
			setParams();
		break;
		case 's':
			setOtherParams();
		break;
		case 'o':
			generateFromImage();
		break;
		case 'c':
			exec.execute(new Runnable(){public void run(){
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
			}});
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

	private void generateFromImage() {
		File f = chooseFile(JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
		PngReaderInt r = new PngReaderInt(f);
		int[] imagePixels = new int[r.imgInfo.samplesPerRow];
		//r.r
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

	private void seedImage(int how) {
		if (how == 0) {
			//addPoint(centerPair)
			for (int i = 0; i < 13; i++) {
				int x = myRandom.nextInt(canvas.getWidth()), y =  myRandom.nextInt(canvas.getHeight());
				operations.addPoint(x,y);
				current[x][y] = true;
				setPixel(x, y, randomColor());
			}
		} else if (how == 1 || how == 2) {
			int c = (how == 1)? Color.BLACK.getRGB(): randomColor();
			int numberOfLines = (canvas.getWidth()*canvas.getHeight())/(LINE_LENGTH*AVERAGE_INVERSE_LINE_DENSITY);
			if (numberOfLines<1) numberOfLines = 1;
			for (int j = 0; j < numberOfLines; j++) {
				//c = randomColor();
				double x = myRandom.nextInt(canvas.getWidth()), y = myRandom.nextInt(canvas.getHeight());
				double r = myRandom.nextInt(255), g = myRandom.nextInt(255), b = myRandom.nextInt(255);
				double rx = myRandom.nextInt(7)-3, ry = myRandom.nextInt(7)-3;
				double rr = myRandom.nextInt(7)-3, rg = myRandom.nextInt(7)-3, rb = myRandom.nextInt(7)-3;
				for (int i = 0; i < LINE_LENGTH; i++) {
					rx = lerp(rx, myRandom.nextInt(7)-3, .05);
					ry = lerp(ry, myRandom.nextInt(7)-3, .05);
					rr = lerp(rr, myRandom.nextInt(7)-3, .05);
					rg = lerp(rg, myRandom.nextInt(7)-3, .05);
					rb = lerp(rb, myRandom.nextInt(7)-3, .05);
					double d = sqrt(pow(rx, 2)+pow(ry, 2));
					double cd = sqrt(pow(rr, 2)+pow(rg, 2)+pow(rb, 2));
					rx=rx/d;
					ry=ry/d;
					rr=rr/cd;
					rg=rg/cd;
					rb=rb/cd;
					x = x+rx;
					y = y+ry;
					r = r+rx;
					g = g+rx;
					b = b+ry;
					//println("("+x+","+y+")");
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
					if (how == 2) c = color((int)r,(int)g,(int)b);
					//c = lerpColor(c,(int)randnum(#FFFFFF),.1);
					//operations.add(new Pair((int)x, (int)y));
					current[(int)x][(int)y] = true;
					setPixel((int)x, (int)y, c);
					if(how == 2) {
						canvas.setRGB((int)x, (int)y, c);
						this.repaint((int)x, (int)y, 1, 1);
					}
				}
			}
		} else if (how == 3) {
			operations.addPoint(centerPair);
		}
		//updatePixels();//I/NFO: updatePixels() for show
	}

	private void fillOperations() {
		for (int x = 0; x < current.length; x++) {
			for (int y = 0; y < current[0].length; y++) {
				if (current[x][y]) {
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
			if (((y+1)<canvas.getHeight())&&!current[x][y+1]) {
				if (RANDOMFACTOR<=0||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) operations.addPoint(x,y+1);
			}
			if (((x+1)<canvas.getWidth())&&!current[x+1][y]) {
				if (RANDOMFACTOR<=0||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) operations.addPoint(x+1, y);
			}
			if (((y-1)>=0)&&!current[x][y-1]) {
				if (RANDOMFACTOR<=0||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) operations.addPoint(x, y-1);
			}
			if (((x-1)>=0)&&!current[x-1][y]) {
				if (RANDOMFACTOR<=0||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) operations.addPoint(x-1, y);
			}
		}
		doneCount++;
		if (doneCount==THREADNUM) {
			synchronized(this) {
				this.notifyAll();
			}
		}
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
		doneCount=0;
		for (int i=0;i<THREADNUM;i++) {
			exec.execute(new Runnable() {
				public void run() {
					//System.out.println("running");
					fillAllPixels();
				}
			});
		}
		/*try{
	   exec.awaitTermination(10,TimeUnit.SECONDS);
	   }catch(InterruptedException ie){}*/
		synchronized(this) {
			try {
				wait();
			}
			catch(InterruptedException ie) {
			}
		}
		//while(doneCount<THREADNUM);
		if(0 != RANDOMFACTOR) finalizePixels(FINALIZATION_METHOD);
		pixnum=0;
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

	private void finalizePixels(int how) {
		switch(how) {
		case 0:
			randomSeedPixels();
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		break; 
		case 1:
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		break;
		case 2:
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) setPixel(x, y, Color.BLACK.getRGB());
				}
			}
		break;
		case 3:
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
		break;
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
			if (maxr>curr) maxr=(curr+posvar);
			if (minr<curr) minr=(curr-negvar);
			if (maxg>curg) maxg=(curg+posvar);
			if (ming<curg) ming=(curg-negvar);
			if (maxb>curb) maxb=(curb+posvar);
			if (minb<curb) minb=(curb-negvar);
		}

		maxr = clamp(0,maxr,255);
		minr = clamp(0,minr,255);
		maxg = clamp(0,maxg,255);
		ming = clamp(0,ming,255);
		maxb = clamp(0,maxb,255);
		minb = clamp(0,minb,255);

		int r=clamp(GREYFACTOR, biasedRandom(minr, maxr, CENTERBIAS, RBIAS), 255-GREYFACTOR);
		int g=clamp(GREYFACTOR, biasedRandom(ming, maxg, CENTERBIAS, GBIAS), 255-GREYFACTOR);
		int b=clamp(GREYFACTOR, biasedRandom(minb, maxb, CENTERBIAS, BBIAS), 255-GREYFACTOR);
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
			if (SHARP) {
				return myRandom.nextBoolean()? minVal: maxVal;
			} else {
				return ((maxVal+minVal)/2);
			}
		}

		double a = myRandom.nextDouble();
		a = .5+(randomListDouble(-.5, .5)*pow(a, biastocenter));
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
