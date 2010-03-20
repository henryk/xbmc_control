package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.game.GameCanvas;

import ch.ploetzli.xbmc.api.HttpApi;

public class RemoteControl extends DatabaseSubMenu {
	/* Constants from key.h */
	static final int KEY_BUTTON_A = 256;
	static final int KEY_BUTTON_DPAD_UP = 270;
	static final int KEY_BUTTON_DPAD_DOWN = 271;
	static final int KEY_BUTTON_DPAD_LEFT = 272;
	static final int KEY_BUTTON_DPAD_RIGHT = 273;
	
	public RemoteControl(String name) {
		super(name);
	}

	protected Displayable constructDisplayable() {
		Displayable canvas = new RemoteControlCanvas(name);
		canvas.addCommand(backCommand);
		canvas.setCommandListener(this);
		return canvas;
	}
	
	public void sendKey(int buttoncode) {
		DatabaseTopMenu topMenu = getDatabaseTopMenu();
		if(topMenu == null)
			return;
		HttpApi api = topMenu.getApi();
		if(api == null)
			return;
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
	
	protected class RemoteControlCanvas extends GameCanvas {

		protected RemoteControlCanvas(String name) {
			super(false);
			setTitle(name);
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
