package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.StateListener;

public class RemoteControl extends DatabaseSubMenu implements StateListener {
	/* Constants from key.h */
	static final int KEY_BUTTON_A = 256;
	static final int KEY_BUTTON_DPAD_UP = 270;
	static final int KEY_BUTTON_DPAD_DOWN = 271;
	static final int KEY_BUTTON_DPAD_LEFT = 272;
	static final int KEY_BUTTON_DPAD_RIGHT = 273;
	static Command tabCommand = new Command("Tab", Command.ITEM, 10);
	
	public RemoteControl(String name) {
		super(name);
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().registerListener(this);
		}
	}

	protected Displayable constructDisplayable() {
		Displayable canvas = new RemoteControlCanvas(name);
		addPrivateCommands(canvas);
		canvas.setCommandListener(this);
		return canvas;
	}
	
	protected void addPrivateCommands(Displayable d) {
		d.addCommand(tabCommand);
		super.addPrivateCommands(d);
	}
	
	public void commandAction(Command cmd, Displayable d) {
		if(cmd == tabCommand) {
			sendKey(0xF009);
		} else {
			super.commandAction(cmd, d);
		}
	}
	
	public void sendKey(int buttoncode) {
		HttpApi api = getApi();
		new Thread() {
			private int buttoncode;
			private HttpApi api;
			public void run() {
				try {
					api.sendKey(buttoncode);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			public void start(HttpApi api, int buttoncode) {
				this.api = api; this.buttoncode = buttoncode;
				super.start();
			}
		}.start(api, buttoncode);
	}

	private HttpApi getApi() {
		DatabaseTopMenu topMenu = getDatabaseTopMenu();
		if(topMenu == null)
			return null;
		HttpApi api = topMenu.getApi();
		if(api == null)
			return null;
		return api;
	}
	
	private void pause() {
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().unregisterListener(this);
		}
	}
	
	private void unpause() {
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().registerListener(this);
		}
	}
	
	protected class RemoteControlCanvas extends GameCanvas implements StateListener {

		protected RemoteControlCanvas(String name) {
			super(false);
			drawBackground(getWidth(), getHeight());
			setTitle(name);
		}
		
		private void drawBackground(int w, int h) {
			Graphics g = getGraphics();
			g.setClip(0, 0, w, h);
			int width = g.getClipWidth();
			int height = g.getClipHeight();
			
			g.setColor(0, 0, 0);
			g.fillRect(0, 0, width, height);
			
			final int iterations = 10;
			for(int i = 0; i<=iterations; i++) {
				g.setColor(i*255/iterations, i*255/iterations, i*255/iterations);
				g.fillRoundRect(i, i, width-2*i, height-2*i, iterations, iterations);
			}
		}

		protected void sizeChanged(int w, int h) {
			super.sizeChanged(w, h);
			drawBackground(w, h);
		}
		
		protected void hideNotify() {
			pause();
			super.hideNotify();
		}
		
		protected void showNotify() {
			unpause();
			super.hideNotify();
		}
		
		private void actOnKey(int keyCode) {
			setTitle(Integer.toString(keyCode));
			if(getGameAction(keyCode) == FIRE) {
				sendKey(KEY_BUTTON_A);
			} else if(getGameAction(keyCode) == UP) {
				sendKey(KEY_BUTTON_DPAD_UP);
			} else if(getGameAction(keyCode) == DOWN) {
				sendKey(KEY_BUTTON_DPAD_DOWN);
			} else if(getGameAction(keyCode) == LEFT) {
				sendKey(KEY_BUTTON_DPAD_LEFT);
			} else if(getGameAction(keyCode) == RIGHT) {
				sendKey(KEY_BUTTON_DPAD_RIGHT);
			} else {
				if( (keyCode >= 'a' && keyCode <= 'z')) {
					/* For ASCII characters the interface wants uppercase,
					 * even though that isn't mentioned in the documentation anywhere
					 */
					sendKey(0xF100 + keyCode - 'a' + 'A');
				} else if( (keyCode > 0 && keyCode <= 127) ) {
					sendKey(0xF000 + keyCode);
				}
			}
		}
		
		protected void keyPressed(int keyCode) {
			System.out.println("Pressed: " + getKeyName(keyCode));
			super.keyPressed(keyCode);
		}
		protected void keyReleased(int keyCode) {
			System.out.println("Released: " + getKeyName(keyCode));
			actOnKey(keyCode);
			super.keyReleased(keyCode);
		}
		protected void keyRepeated(int keyCode) {
			System.out.println("Repeated: " + getKeyName(keyCode));
			actOnKey(keyCode);
			super.keyRepeated(keyCode);
		}
	}
	
}
