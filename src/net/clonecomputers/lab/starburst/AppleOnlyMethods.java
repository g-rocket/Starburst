package net.clonecomputers.lab.starburst;

import javax.swing.*;

import com.apple.eawt.*;

public class AppleOnlyMethods {
	@SuppressWarnings("restriction")
	public static void appleFullscreen(JFrame window) {
		window.setUndecorated(true);
		window.pack();
		window.setVisible(true);
		window.setAutoRequestFocus(true);
		FullScreenUtilities.addFullScreenListenerTo(window,new com.apple.eawt.FullScreenAdapter(){
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
		FullScreenUtilities.setWindowCanFullScreen(window,true); //TODO: test compiling on non-mac
		Application.getApplication().requestToggleFullScreen(window);//TODO: compiles non-mac?
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
	}
	
	@SuppressWarnings("restriction")
	public static void appleForeground() {
		Application.getApplication().requestForeground(true);
	}
}
