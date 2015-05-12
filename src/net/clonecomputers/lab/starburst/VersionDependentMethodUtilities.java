package net.clonecomputers.lab.starburst;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import com.apple.eawt.*;

public class VersionDependentMethodUtilities {

	public static void initLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedLookAndFeelException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int javaVersion() {
		return Integer.parseInt(System.getProperty("java.specification.version").split("[.]")[1]);
	}

	public static boolean appleEawtAvailable() {
		return System.getProperty("os.name").contains("OS X");
	}
	
	public static int appleOSVersion() {
		if(!appleEawtAvailable()) return -1;
		return Integer.parseInt(System.getProperty("os.version").split("[.]")[1]);
	}
	
	public static void setAutoRequestFocus(JFrame window, boolean autoRequestFocus) {
		try {
			Method setAutoRequestFocusMethod = window.getClass().getMethod("setAutoRequestFocus", boolean.class);
			setAutoRequestFocusMethod.invoke(window, autoRequestFocus);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void toFullScreen(JFrame window, GraphicsDevice gd, boolean tryAppleFullscreen, boolean tryExclusiveFullscreen){
		if(appleEawtAvailable() && 
				tryAppleFullscreen && 
				appleOSVersion() >= 7 && // lion and above
				javaVersion() >= 7){ // java 7 and above
			System.out.println("trying to apple fullscreen");
			enableAppleFullscreen(window);
			doAppleFullscreen(window);
		} else if(appleEawtAvailable() && // Snow Leopard and below OR apple java 6 and below TODO: test this on SL
				tryExclusiveFullscreen &&
				gd.isFullScreenSupported()){
			if(javaVersion() >= 7) setAutoRequestFocus(window, true);
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setUndecorated(true);
			//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			gd.setFullScreenWindow(window);
			window.toFront();
			Rectangle r = gd.getDefaultConfiguration().getBounds();
			window.setBounds(r);
			//window.pack();
		} else { // Windows and Linux TODO: test this
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setUndecorated(true);
			window.setSize(gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight());
			window.setLocation(0, 0);
			window.setExtendedState(JFrame.MAXIMIZED_BOTH);
			window.toFront();
		}
		window.pack();
		window.setVisible(true);
	}

	public static void enableFullscreen(JFrame window) {
		enableFullscreen(window, false);
	}
	
	public static void enableFullscreen(final JFrame window, final boolean useExclusiveFullscreen) {
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if(appleEawtAvailable() && 
				appleOSVersion() >= 7 && // lion and above
				javaVersion() >= 7){ // java 7 and above
			System.out.println("trying to apple fullscreen");
			enableAppleFullscreen(window);
		}
		window.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_F11) {
					e.consume();
					GraphicsDevice gd = getScreen(window);
					window.setVisible(false); // hide, so we can do stuff
					if(window.isUndecorated()) { // we are fullscreen, we should become unfullscreen
						window.setUndecorated(false);
						if(window.equals(gd.getFullScreenWindow())) { // we used exclusive mode to go fullscreen
							gd.setFullScreenWindow(null);
						} else { // we just got really big
							window.setExtendedState(JFrame.NORMAL);
						}
					} else { // we aren't fullscreen, we should become fullscreen
						if(javaVersion() >= 7) setAutoRequestFocus(window, true);
						window.setUndecorated(true);
						window.setBounds(gd.getDefaultConfiguration().getBounds());
						if(useExclusiveFullscreen) {
							gd.setFullScreenWindow(window);
						} else {
							window.setExtendedState(JFrame.MAXIMIZED_BOTH);
							window.toFront();
						}
					}
					window.pack();
					window.setVisible(true);
				}
			}
		});
	}
	
	private static GraphicsDevice getScreen(JFrame window) {
		for(GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			if(gd.getDefaultConfiguration().getBounds().contains(window.getLocationOnScreen())) return gd;
		}
		System.err.println(window+" does not appear to be on any screen; fullscreening onto default screen");
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	@SuppressWarnings("restriction")
	public static void enableAppleFullscreen(JFrame window) {
		window.pack();
		window.setVisible(true);
		setAutoRequestFocus(window, true);
		if(javaVersion() == 7) { // work around bug in java 7 where top bar space is blank unless undecorated
			FullScreenUtilities.addFullScreenListenerTo(window,new FullScreenAdapter(){
				boolean working = false;
				@Override
				public void windowEnteredFullScreen(AppEvent.FullScreenEvent e) {
					if(working){
						working = false;
						return;
					};
					if(!((JFrame)e.getWindow()).isUndecorated()){
						working = true;
						Application.getApplication().requestToggleFullScreen(e.getWindow());
					}
				}
				@Override
				public void windowExitedFullScreen(AppEvent.FullScreenEvent e) {
					if(working){
						e.getWindow().dispose();
						((JFrame)e.getWindow()).setUndecorated(true);
						e.getWindow().pack();
						e.getWindow().setVisible(true);
						Application.getApplication().requestToggleFullScreen(e.getWindow());
						return;
					};
					if(((JFrame)e.getWindow()).isUndecorated()){
						e.getWindow().dispose();
						((JFrame)e.getWindow()).setUndecorated(false);
						e.getWindow().setVisible(true);
					}
				}
			});
		}
		FullScreenUtilities.setWindowCanFullScreen(window,true);
	}

	@SuppressWarnings("restriction")
	public static void doAppleFullscreen(JFrame window) {
		Application.getApplication().requestToggleFullScreen(window);
	}

	@SuppressWarnings("restriction")
	public static void appleForeground() {
		Application.getApplication().requestForeground(true);
	}
}
