package net.clonecomputers.lab.starburst;

import java.awt.*;
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
	
	public static void toFullScreen(JFrame window, GraphicsDevice gd, boolean tryAppleFullscreen){
		if(appleEawtAvailable()) {
			if(tryAppleFullscreen && 
			   appleOSVersion() >= 7 && // lion and above
			   javaVersion() >= 7){ // java 7 and above
				System.out.println("trying to apple fullscreen");
				appleFullscreen(window);
			} else { // Snow Leopard and below OR apple java 6 and below TODO: test this on SL
				if(javaVersion() >= 7) setAutoRequestFocus(window, true);
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setUndecorated(true);
				//window.setExtendedState(JFrame.MAXIMIZED_BOTH);
				gd.setFullScreenWindow(window);
				window.toFront();
				Rectangle r = gd.getDefaultConfiguration().getBounds();
				window.setBounds(r);
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

	@SuppressWarnings("restriction")
	public static void appleFullscreen(JFrame window) {
		//window.setUndecorated(true);
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
		FullScreenUtilities.setWindowCanFullScreen(window,true); //TODO: test compiling on non-mac
		Application.getApplication().requestToggleFullScreen(window);//TODO: compiles non-mac?
	}

	@SuppressWarnings("restriction")
	public static void appleForeground() {
		Application.getApplication().requestForeground(true);
	}
}
