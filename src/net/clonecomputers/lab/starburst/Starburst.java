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
import javax.imageio.stream.*;
import javax.swing.*;

public class Starburst extends JDesktopPane {
	//final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
	Random myRandom = new Random();
	BufferedImage canvas;

	int negvar = 15;
	int posvar = 15;

	boolean looping = false;

	int doneCount=0;

	int FINALIZATION_METHOD = 3;
	//0 -> squares then loop x,y
	//1 -> loop x,y
	//2 -> fill with black
	//3 -> run almost normal program from center
	//4 -> do nothing

	int SEED_METHOD = 2;
	//0 -> 13 random points
	//1 -> black lines
	//2 -> colored lines
	//3 -> center point

	//boolean RANDOMFACTOR<0 = true;

	boolean GEN_ONLY_ONE_THEN_SAVE_AND_EXIT_MODE = false;

	String SAVE_DIRECTORY = "/usr/share/images/clone-desktop/starburst-backgrounds/";

	boolean RANDOMPROPERTIES = true; // randomize image properties
	//each time w/in a list of good values

	boolean RANDOM_OTHER_PROPS = true; // randomize seed and
	//finalization properties each time w/in a list of good values

	static int LINE_LENGTH = 1000;// length of a line
	// 10000 gives a good length, usually, I think
	// there is a lot of turning and doubling back, 
	// so the lines are actually much shorter
	static int AVERAGE_INVERSE_LINE_DENSITY = 300;
	// number of lines = (w*h)/(LINE_LENGTH*AVERAGE_INVERSE_LINE_DENSITY)

	static boolean SHARP = true; //whether to average when 
	//no possible values are found or to choose a random endpoint

	static double RBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker
	static double GBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker
	static double BBIAS = 0; //0 is no bias.  
	// higher numbers for lighter, lower numbers for darker
	static double BWBIAS = (RBIAS+GBIAS+BBIAS)/3;//0 is no bias.  
	// higher numbers for lighter, lower numbers for darker

	static double CENTERBIAS = 10; //1 is no bias, higher means more towards center
	// bigger numbers also take longer to make an image, but mean more toned down

	static int GREYFACTOR = 0;//0 is no bias.  
	// bigger numbers for greyer, up to 127 for all grey

	static double RANDOMFACTOR = 1.05;
	// the probabiity check that determines if a cell should be included
	// return true RANDOMFACTOR / (RANDOMFACTOR + 1) of the time
	// 0 is 100% chance

	static int genNum = 30;

	static int THREADNUM = 15;

	static final String PARAM_CHANGE_MESSAGE = 
			"Please input the image generation paramaters in the form" + "\n"
					+ "(red bias, green bias, blue bias, center bias, grey factor, random layout factor)" + "\n"
					+ "or 'random' for random values or 'same' for current values, or cancel to exit unchanged";
	static final String OTHER_PARAM_CHANGE_MESSAGE = 
			"Please input the seeding and finalizations paramaters in the form" + "\n"
					+ "(seed method, finalization method)" + "\n"
					+ "or 'random' for random values or 'same' for current values, or cancel to exit unchanged" + "\n"
					+ "# |  Seed Method  | Finalization" + "\n"
					+ "-----------------------------------" + "\n"
					+ "0 |   13 points   | squares then loop x,y" + "\n"
					+ "1 |  black lines  | loop x,y" + "\n"
					+ "2 | colored lines | fill with black" + "\n"
					+ "3 | center point  | run normally from center point" + "\n";
	int pixnum=0;
	boolean current[][];
	List<Pair> opperations;
	ExecutorService exec = Executors.newFixedThreadPool(THREADNUM);
	Pair centerPair;
	private int[] pixels;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable(){@Override public void run(){
			GraphicsDevice[] gda = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			GraphicsDevice gd = null;
			int maxPixelsSoFar = 0;
			for(GraphicsDevice g: gda){ // find biggest screen
				DisplayMode d = g.getDisplayMode();
				if(d.getWidth() * d.getHeight() > maxPixelsSoFar){
					gd = g;
					maxPixelsSoFar = d.getWidth() * d.getHeight();
				}
			}
			DisplayMode d = gd.getDisplayMode();
			JFrame window = new JFrame();
			//window.setBackground(Color.WHITE);
			//window.setForeground(Color.WHITE);
			Dimension size;
			if(args.length == 0) {
				size = new Dimension(d.getWidth(), d.getHeight());
			} else if(args.length == 1) {
				size = new Dimension(
					Integer.parseInt(args[0].split(",")[0].trim()),
					Integer.parseInt(args[0].split(",")[1].trim()));
			} else if(args.length == 2) {
				size = new Dimension(
						Integer.parseInt(args[0].trim()),
						Integer.parseInt(args[1].trim()));
			} else try { // args.length > 2 means read from stdin
				System.out.println("Input dimensions");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String arg0 = reader.readLine().trim();
				if(arg0.matches("\\d+\\s*,\\s*\\d+")){
					size = new Dimension(
							Integer.parseInt(arg0.split(",")[0].trim()),
							Integer.parseInt(arg0.split(",")[1].trim()));
				} else if(arg0.matches("\\d+")) {
					String arg1 = reader.readLine().trim();
					size = new Dimension(
							Integer.parseInt(arg0.trim()),
							Integer.parseInt(arg1.trim()));
				} else {
					throw new IllegalArgumentException("Invalid dimensions");
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			final Starburst s = new Starburst(size);
			
			window.addMouseListener(new MouseAdapter(){
				@Override public void mouseClicked(MouseEvent e){
					s.mousePressed();
				}
			});
			s.addMouseListener(new MouseAdapter(){
				@Override public void mouseClicked(MouseEvent e){
					s.mousePressed();
				}
			});
			window.addKeyListener(new KeyAdapter(){
				/*@Override public void keyReleased(KeyEvent e){
					if(e.getKeyCode() == KeyEvent.VK_ENTER) s.mousePressed();
					//if(e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
				}*/
				@Override public void keyTyped(KeyEvent e){
					s.keyPressed(e.getKeyChar());
				}
			});
			s.addKeyListener(new KeyAdapter(){
				/*@Override public void keyReleased(KeyEvent e){
					if(e.getKeyCode() == KeyEvent.VK_ENTER) s.mousePressed();
					//if(e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
				}*/
				@Override public void keyTyped(KeyEvent e){
					s.keyPressed(e.getKeyChar());
				}
			});
			window.setContentPane(s);
			toFullScreen(window,gd);
			s.newImage();
		}});
	}

	@SuppressWarnings("restriction")
	public static void toFullScreen(JFrame window, GraphicsDevice gd){
		if(System.getProperty("os.name").contains("OS X")) {
			if(Integer.parseInt(System.getProperty("os.version").split("[.]")[1]) >= 7 && // lion and above
			   Integer.parseInt(System.getProperty("java.specification.version").split("[.]")[1]) >= 7){ // java 7 and above
				System.out.println("trying to apple fullscreen");
				window.setUndecorated(true);
				window.pack();
				window.setVisible(true);
				com.apple.eawt.FullScreenUtilities.addFullScreenListenerTo(window,new com.apple.eawt.FullScreenAdapter(){
					boolean working = false;
					@Override
					public void windowEnteredFullScreen(com.apple.eawt.AppEvent.FullScreenEvent e) {
						if(working){
							working = false;
							return;
						};
						if(!((JFrame)e.getWindow()).isUndecorated()){
							working = true;
							com.apple.eawt.Application.getApplication().requestToggleFullScreen(e.getWindow());
						}
					}
					@Override
					public void windowExitedFullScreen(com.apple.eawt.AppEvent.FullScreenEvent e) {
						if(working){
							e.getWindow().dispose();
							((JFrame)e.getWindow()).setUndecorated(true);
							e.getWindow().pack();
							e.getWindow().setVisible(true);
							com.apple.eawt.Application.getApplication().requestToggleFullScreen(e.getWindow());
							return;
						};
						if(((JFrame)e.getWindow()).isUndecorated()){
							e.getWindow().dispose();
							((JFrame)e.getWindow()).setUndecorated(false);
							e.getWindow().setVisible(true);
						}
					}
				});
				com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(window,true); //TODO: test compiling on non-mac
				com.apple.eawt.Application.getApplication().requestToggleFullScreen(window);//TODO: compiles non-mac?
				/*try {
					Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
					Class<?>[] params = new Class[]{Window.class, Boolean.TYPE};
					Method method = util.getMethod("setWindowCanFullScreen", params);
					method.invoke(util, window, true);
					Class<?> application = Class.forName("com.apple.eawt.Application");
					Method fullscreenMethod = application.getMethod("requestToggleFullScreen", new Class[]{Window.class});
					fullscreenMethod.invoke(
							application.getMethod("getApplication", new Class[0]).invoke(null), window);
					window.pack();
					window.setVisible(true);
					return;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}*/
			} else { // Snow Leopard and below TODO: test this
				if(Integer.parseInt(System.getProperty("java.version").split("[.]")[1]) >= 7){
					try{
						Class<? extends JFrame> windowClass = window.getClass();
						Method setAutoRequestFocusMethod = windowClass.getMethod("setAutoRequestFocus", boolean.class);
						setAutoRequestFocusMethod.invoke(window, true);
					} catch(InvocationTargetException e) {
						// will never happen
					} catch(NoSuchMethodException e) {
						// can't find it, oh well.  cant call on <7 anyways
					} catch(IllegalAccessException e) {
						// will never happen
					}
				}
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setUndecorated(true);
				//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
				gd.setFullScreenWindow(window);
				//window.setVisible(false);
				window.toFront();
				//window.setAlwaysOnTop(true);
				//window.toFront();
				Rectangle r = gd.getDefaultConfiguration().getBounds();
				window.setBounds(r);
				//window.toFront();
				//window.setAlwaysOnTop(true);
				//window.pack();
			}
		} else { // Windows and Linux TODO: test this
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setUndecorated(true);
			window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.toFront();
		}
		window.pack();
		window.setVisible(true);
	}

	public Starburst(int w,int h){
		//fc.setUI(new )
		/*try{
	   javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
	   }catch(Exception e){
	   throw new RuntimeException(e);
	   }*/
		pixels = new int[w*h];
		canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		this.setPreferredSize(new Dimension(w,h));
		opperations = Collections.synchronizedList(new LinkedList<Pair>());
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

	void genMany(String outputDirectory, int howMany) {
		for (int i=0;i<genNum;i++) {
			newImage();
			saveRandomName(outputDirectory);
		}
		System.exit(0);
	}

	void saveRandomName(String outputDirectory) {
		String filename = String.format("%s/%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d %s.png",
				outputDirectory,
				RBIAS, GBIAS, BBIAS, CENTERBIAS-1, GREYFACTOR,
				RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD, randomstr(8));
		save(filename);
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

	private void save(String filename) {
		File f = new File(filename);
		String[] fileParts = filename.split("\\.");
		String suffix = fileParts[fileParts.length-1];
		ImageWriter i;
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
		}
	}

	void randomizeProperties() {
		RBIAS = randoml(randomr(-.5, .5), randomr(-.5, .5), randomr(-1.5, -.5));
		GBIAS = randoml(randomr(-.5, .5), randomr(-.5, .5), randomr(-1.5, -.5));
		BBIAS = randoml(randomr(-.5, .5), randomr(-.5, .5), randomr(-1.5, -.5));
		BWBIAS = (RBIAS+GBIAS+BBIAS)/3;
		CENTERBIAS = randoml(randomr(0, 15), randomr(10, 15), 0)+1;
		RANDOMFACTOR = randoml(randomr(0, 1), randomr(0, 1), randomr(1, 2), randomr(0, 3), 20, 100);
	}

	void randomizeOtherProperties() {
		SEED_METHOD = (int)randoml(0, 1, 2, 3);
		FINALIZATION_METHOD = (int)randoml(0, 1, 2, 3);
	}

	double randomr(double min, double max) {
		return myRandom.nextInt((int)(max-min))+min;
	}

	double randoml(double ... list) {
		return list[myRandom.nextInt(list.length)];
	}

	void newImage() {
		System.out.println("newImage");
		System.out.println(String.format("%.2f, %.2f, %.2f, %.2f, %d, %.2f, %d, %d", 
				RBIAS, GBIAS, BBIAS, CENTERBIAS, 
				GREYFACTOR, RANDOMFACTOR, SEED_METHOD, FINALIZATION_METHOD));
		if (RANDOMPROPERTIES) randomizeProperties();
		if (RANDOM_OTHER_PROPS) randomizeOtherProperties();
		loadPixels();
		falsifyCurrent();
		seedImage(SEED_METHOD);
		System.out.println("done seeding");
		fillOperations();
		fillAll();
		System.out.println("done generating");
		//loop();
		updatePixels();
		//cancelPhantomProcessorUsage();
		System.out.println("end newImage");
	}

	private void updatePixels() {
		int i = 0;
		for(int y = 0; y < canvas.getHeight(); y++){
			for(int x = 0; x < canvas.getWidth(); x++){
				canvas.setRGB(x, y, pixels[i++]);
			}
		}
		this.repaint();
	}

	private void loadPixels() {
		pixels = ((DataBufferInt) canvas.getData().getDataBuffer()).getData();
	}

	@Override public void paintComponent(Graphics g){
		super.paintComponent(g);
		g.drawImage(canvas, 0, 0, this);
	}

	void setOtherParams() {
		String curprops = String.format("%d, %d", 
				SEED_METHOD, FINALIZATION_METHOD);
		System.out.println(curprops);
		String input = javax.swing.JOptionPane.showInternalInputDialog(this,OTHER_PARAM_CHANGE_MESSAGE);
		if (input==null) return;
		if (input.equalsIgnoreCase("random")) {
			RANDOM_OTHER_PROPS = true;
			newImage();
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
			FINALIZATION_METHOD = Integer.parseInt(params[1].trim());
		}
		catch(Exception e) {
			System.out.println(e+" in setParams()");
		}
		RANDOM_OTHER_PROPS = false;
		newImage();
	}

	void setParams() {
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
			newImage();
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
		newImage();
	}

	void keyPressed(char key) {
		if(key=='v'||key=='V') mousePressed();
		else if (key=='p'||key=='P') setParams();
		else if (key=='s'||key=='S') setOtherParams();
		/*else if (key=='d'||key=='D') {
			String input = javax.swing.JOptionPane.showInputDialog(this, "Input your new dimensions");
			String[] dims = input.split(",");
			size(Integer.parseInt(dims[0]),Integer.parseInt(dims[1]));
			current = new boolean[w][h];
			centerPair = new Pair(w/2, h/2);
			loadPixels();
			falsifyCurrent();
			seedImage();
			fillOperations();
			fillAll();
			updatePixels();
		}*/
		/*else if(key=='y'||key=='Y'){
			looping = !looping;
			if(looping){
				println("looping");
				loop();
			}else{
				println("not looping");
				noLoop();
			}
		}*/
		else if (key=='m'||key=='M') {
			exec.execute(new Runnable(){@Override public void run(){
				String input = javax.swing.JOptionPane.showInternalInputDialog(Starburst.this,
						"How many images do you want to generate?");
				if (input == null) return;
				File outputDirectory = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.DIRECTORIES_ONLY).getParentFile();
				if(outputDirectory == null) return;
				System.out.println("about to gen");
				genMany(outputDirectory.getAbsolutePath(), Integer.parseInt(input));
			}});
		} 
		else if (key != 27) newImage();
	}
	
	private File chooseFile(int dialogType, int selectionMode) {
		final JInternalFrame fcFrame = new JInternalFrame();
		fcFrame.putClientProperty("JInternalFrame.frameType", "optionDialog");
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(selectionMode);
		fc.setDialogType(dialogType);
		fc.setMultiSelectionEnabled(false);
		fc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (JFileChooser.CANCEL_SELECTION.equals(cmd)) {
					fcFrame.setVisible(false);
					synchronized(fc) {
						fc.notifyAll();
					}
				} else if (JFileChooser.APPROVE_SELECTION.equals(cmd)) {
					fcFrame.setVisible(false);
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
		return(fc.getSelectedFile());
	}

	void mousePressed() {
		System.out.println("mousePressed");
		//String savePath = selectOutput("select an output file");  // Opens file chooser
		/*fc.showSaveDialog(this);
		if (fc.getSelectedFile()==null) return;
		String savePath = fc.getSelectedFile().getAbsolutePath();*/
		exec.execute(new Runnable(){@Override public void run(){
			File output = chooseFile(JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
			//Starburst.this.requestFocus();
			if(output == null) return;
			String savePath = output.getAbsolutePath();
			System.out.println("file selected");
			if (savePath == null) {
				// If a file was not selected
				System.out.println("No output file was selected...");
			} 
			else {
				//if (!savePath.endsWith(".png")) savePath+=".png";
				// If a file was selected, save image to path
				System.out.println("saving to "+savePath);
				save(savePath);
				System.out.println("saved");
			}
		}});
	}

	int randomColor() {
		return new Color(myRandom.nextInt(255),myRandom.nextInt(255),myRandom.nextInt(255)).getRGB();
	}

	void seedImage(int how) {
		if (how == 0) {
			//opperations.add(centerPair);
			for (int i = 0; i < 13; i++) {
				Pair p = new Pair(myRandom.nextInt(canvas.getWidth()), myRandom.nextInt(canvas.getHeight()));
				opperations.add(p);
				current[p.x][p.y] = true;
				setPixel(p.x, p.y, randomColor());
			}
		} 
		else if (how == 1 || how == 2) {
			int c = (how == 1)? Color.BLACK.getRGB(): randomColor();
			int numberOfLines = (canvas.getWidth()*canvas.getHeight())/(LINE_LENGTH*AVERAGE_INVERSE_LINE_DENSITY);
			if (numberOfLines<1) numberOfLines = 1;
			for (int j = 0; j < numberOfLines; j++) {
				//c = randomColor();
				double x = myRandom.nextInt(canvas.getWidth()), y = myRandom.nextInt(canvas.getHeight());
				double r = myRandom.nextInt(255), g = myRandom.nextInt(255), b = myRandom.nextInt(255);
				double rx = myRandom.nextInt(7)-3, ry = myRandom.nextInt(7)-3;
				double rr = myRandom.nextInt(7)-3, rg = myRandom.nextInt(7)-3, rb = myRandom.nextInt(6)-3;
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
					//opperations.add(new Pair((int)x, (int)y));
					current[(int)x][(int)y] = true;
					setPixel((int)x, (int)y, c);
				}
			}
		} 
		else if (how == 3) {
			opperations.add(new Pair(canvas.getWidth()/2, canvas.getHeight()/2));
		}
	}

	void fillOperations() {
		for (int x = 0; x < current.length; x++) {
			for (int y = 0; y < current[0].length; y++) {
				if (current[x][y]) {
					if (x+1<current.length && !current[x+1][y]) opperations.add(new Pair(x+1, y));
					if (x-1>=0 && !current[x-1][y]) opperations.add(new Pair(x-1, y));
					if (y+1<current[0].length && !current[x][y+1]) opperations.add(new Pair(x, y+1));
					if (y-1>=0 && !current[x][y-1]) opperations.add(new Pair(x, y-1));
				}
			}
		}
	}

	int getPixel(int x, int y) {
		return pixels[x+(y*canvas.getWidth())];
	}

	void setPixel(int x, int y, int c) {
		pixels[x+(y*canvas.getWidth())]=c;
	}

	double lerp(double a, double b, double lerpVal){
		return a + b*lerpVal;
	}

	void falsifyCurrent() {
		for (int i=0;i<canvas.getWidth();i++) {
			for (int j=0;j<canvas.getHeight();j++) {
				current[i][j]=false;
			}
		}
	}

	synchronized Pair getNextObject() {
		if (!opperations.isEmpty()) {
			int index = RANDOMFACTOR<0? myRandom.nextInt(opperations.size()): 0;
			return opperations.remove(index);
		}
		else {
			return null;
		}
		//  Pair retval=null;
		//  while (opperations.size()>0&&(retval=opperations.remove(0/*(int)(Math.random()*opperations.size())*/))==null);
		//  return retval;
	}

	void fillAllPixels() {
		while (!opperations.isEmpty()) {
			Pair myPair=getNextObject();
			if (myPair==null) continue;
			int x=myPair.x, y=myPair.y;
			if (current[x][y]) continue;
			fillPixel(x, y);
			boolean iscpx = false;//(x==centerPair.x&&y==centerPair.y);
			if (((y+1)<canvas.getHeight())&&!current[x][y+1]) {
				if (RANDOMFACTOR<0||iscpx||(RANDOMFACTOR==0)||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) opperations.add(new Pair(x, y+1));
			}
			if (((x+1)<canvas.getWidth())&&!current[x+1][y]) {
				if (RANDOMFACTOR<0||iscpx||(RANDOMFACTOR==0)||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) opperations.add(new Pair(x+1, y));
			}
			if (((y-1)>=0)&&!current[x][y-1]) {
				if (RANDOMFACTOR<0||iscpx||(RANDOMFACTOR==0)||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) opperations.add(new Pair(x, y-1));
			}
			if (((x-1)>=0)&&!current[x-1][y]) {
				if (RANDOMFACTOR<0||iscpx||(RANDOMFACTOR==0)||myRandom.nextDouble()*(RANDOMFACTOR+1)>1) opperations.add(new Pair(x-1, y));
			}
		}
		doneCount++;
		if (doneCount==THREADNUM) {
			synchronized(this) {
				this.notifyAll();
			}
		}
	}

	void fillAll() {
		doneCount=0;
		for (int i=0;i<THREADNUM;i++) {
			exec.execute(new Runnable() {
				public void run() {
					//System.out.println("running");
					fillAllPixels();
				}
			}
					);
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
		finalizePixels(FINALIZATION_METHOD);
		pixnum=0;
	}

	void randomSeedPixels() {
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

	void finalizePixels(int how) {
		if (how==0) {
			randomSeedPixels();
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		} 
		else if (how==1) {
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) fillPixel(x, y);
				}
			}
		} 
		else if (how==2) {
			for (int x=0;x<canvas.getWidth();x++) {
				for (int y=0;y<canvas.getHeight();y++) {
					if (!current[x][y]) setPixel(x, y, Color.BLACK.getRGB());
				}
			}
		} 
		else if (how==3) {
			boolean[][] localcurrent = new boolean[canvas.getWidth()][canvas.getHeight()];
			opperations.add(centerPair);
			while (!opperations.isEmpty()) {
				Pair myPair=getNextObject();
				int x=myPair.x, y=myPair.y;
				if (localcurrent[x][y]) continue;
				if (!current[x][y]) fillPixel(x, y);
				if (((y+1)<canvas.getHeight())&&!localcurrent[x][y+1]) {
					opperations.add(new Pair(x, y+1));
				}
				if (((x+1)<canvas.getWidth())&&!localcurrent[x+1][y]) {
					opperations.add(new Pair(x+1, y));
				}
				if (((y-1)>=0)&&!localcurrent[x][y-1]) {
					opperations.add(new Pair(x, y-1));
				}
				if (((x-1)>=0)&&!localcurrent[x-1][y]) {
					opperations.add(new Pair(x-1, y));
				}
				localcurrent[x][y]=true;
			}
		}
	}

	void printloc(int x, int y) {
		System.out.print("("+x+","+y+")");
	}

	void fillPixel(int x, int y) {
		int maxr=255, minr=0, maxg=255, ming=0, maxb=255, minb=0;
		int neighborColors[]=new int[5];
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

		if (maxr<0) maxr=0;
		if (maxr>255) maxr=255;
		if (minr<0) minr=0;
		if (minr>255) minr=255;
		if (maxg<0) maxg=0;
		if (maxg>255) maxg=255;
		if (ming<0) ming=0;
		if (ming>255) ming=255;
		if (maxb<0) maxb=0;
		if (maxb>255) maxb=255;
		if (minb<0) minb=0;
		if (minb>255) minb=255;

		int r=bound(biasedRandom(minr, maxr, CENTERBIAS, RBIAS), GREYFACTOR, 255-GREYFACTOR);
		int g=bound(biasedRandom(ming, maxg, CENTERBIAS, GBIAS), GREYFACTOR, 255-GREYFACTOR);
		int b=bound(biasedRandom(minb, maxb, CENTERBIAS, BBIAS), GREYFACTOR, 255-GREYFACTOR);
		setPixel(x, y, color(r, g, b));
		current[x][y]=true;
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

	int bound(int x, int min, int max) {
		return min(max(x, min), max);
	}

	int biasedRandom(int minVal, int maxVal, double biastocenter, double biasFactor) {

		if (maxVal<minVal) {
			if (!SHARP) return ((maxVal+minVal)/2);
			//else if (randnum(2)>1) return minVal;
			else return maxVal;
		}

		double a = myRandom.nextDouble();
		a = .5+(randoml(-.5, .5)*pow(a, biastocenter));
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
