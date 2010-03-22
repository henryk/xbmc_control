package ch.ploetzli.xbmc.j2me;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

import ch.ploetzli.xbmc.Utils;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;
import ch.ploetzli.xbmc.api.StateListener;
import ch.ploetzli.xbmc.api.StateMonitor;

public class RemoteControl extends DatabaseSubMenu implements StateListener {
	/* Constants from key.h */
	static final int KEY_BUTTON_A = 256;
	static final int KEY_BUTTON_DPAD_UP = 270;
	static final int KEY_BUTTON_DPAD_DOWN = 271;
	static final int KEY_BUTTON_DPAD_LEFT = 272;
	static final int KEY_BUTTON_DPAD_RIGHT = 273;
	static Command tabCommand = new Command("Tab", Command.ITEM, 10);
	private RemoteControlCanvas canvas = null;
	
	public RemoteControl(String name) {
		super(name);
		setupListener();
	}

	protected Displayable constructDisplayable() {
		synchronized(this) {
			if(canvas == null) {
				canvas = new RemoteControlCanvas(name);
				addPrivateCommands(canvas);
				canvas.setCommandListener(this);
			}
		}
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
	
	public void stateSynchronized() {
		if(canvas == null)
			constructDisplayable();
		if(canvas != null)
			canvas.refresh();
	}

	public void valueChanged(String property, String newValue) {
		if(canvas == null)
			constructDisplayable();
		if(canvas != null)
			canvas.setValue(property, newValue);
	}
	
	private void setupListener() {
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().registerListener(this, StateMonitor.INTEREST_PERCENTAGE);
		}
	}
	
	protected abstract class GUIElement {
		protected boolean dirty = false;
		public abstract boolean updateValue(String name, String value);
		public abstract void fetch(HttpApi api, int width, int height);
		public abstract void paint(Graphics g, int width, int height);
		public void setDirty() {
			dirty = true;
		}
		public boolean getDirty() {
			return dirty;
		}
		public boolean sizeChanged(int width, int height) {
			setDirty();
			return getDirty();
		}
	}
	
	protected abstract class StringGUIElement extends GUIElement {
		String value;
		
		public boolean updateValue(String name, String newValue) {
			if(!name.equals(getFieldName()))
				return dirty;
			if( value == null && newValue == null)
				return dirty; /* Nothing to do */
			else if( value != null || newValue != null || !value.equals(newValue)) { 
				value = newValue;
				dirty = true;
			}
			return dirty;
		}
		
		public abstract String getFieldName();
	}
	
	protected abstract class IntegerGUIElement extends GUIElement {
		int value;
		
		public boolean updateValue(String name, String newValue) {
			if(!name.equals(getFieldName()))
				return dirty;
			int newVal = getDefaultValue();
			try {
				newVal = Integer.parseInt(newValue);
			} catch(Exception e) {;}
			if(value != newVal) {
				value = newVal;
				dirty = true;
			}
			return dirty;
		}
		
		public abstract int getDefaultValue();
		public abstract String getFieldName();
	}
	
	protected class TvshowThumb extends StringGUIElement {
		Image thumb = null;
		
		public String getFieldName() {
			return "Show Title";
		}

		public void fetch(HttpApi api, int width, int height) {
			if(dirty) {
				if(api != null && value != null) {
					try {
						RecordSetConnection conn = api.queryVideoDatabase("SELECT strPath,c00 FROM tvshowview WHERE c00 = '"+value+"' LIMIT 1");
						String data[] = new String[]{};
						if(conn.hasMoreElements())
							data = (String[]) conn.nextElement();
						if(data.length > 0) {
							String crc = Utils.crc32(data[0]);
							thumb = ImageFactory.getRemoteImage(api, "special://userdata/Thumbnails/Video/"+ crc.charAt(0) + "/" + crc + ".tbn");
						}

						if(thumb != null) {
							thumb = ImageFactory.scaleImageToFit(thumb,	width-20, (int)(height*0.3));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			dirty = false;
		}

		public void paint(Graphics g, int width, int height) {
			if(thumb != null) {
				g.drawImage(thumb, width/2, 10, Graphics.TOP | Graphics.HCENTER);
			}
		}
		
	}

	protected class FileThumb extends StringGUIElement {
		Image thumb = null;
		
		public String getFieldName() {
			return "Thumb";
		}

		public void fetch(HttpApi api, int width, int height) {
			if(dirty) {
				if(api != null && value != null) {
					try {
						thumb = null;
						try {
							thumb = ImageFactory.getRemoteImage(api, value);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if(thumb != null) {
							thumb = ImageFactory.scaleImageToFit(thumb, (int)(width*0.4), (int)(height*0.5));

						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			dirty = false;
		}

		public void paint(Graphics g, int width, int height) {
			if(thumb != null) {
				g.drawImage(thumb, 10, height-25, Graphics.BOTTOM | Graphics.LEFT);
			}
		}
		
	}
	
	protected class PlaybackProgress extends IntegerGUIElement {

		public int getDefaultValue() {
			return -1;
		}

		public String getFieldName() {
			return "Percentage";
		}

		public void fetch(HttpApi api, int width, int height) {
			return;
		}

		public void paint(Graphics g, int width, int height) {
			int barWidth = 0;
			int maxWidth = width - 20;
			if(value > 100)
				value = 100;
			if(value < -1)
				value = -1;
			if(value != -1) {
				barWidth = (maxWidth * value) / 100;
			}

			g.setColor(0, 0, 0);
			g.fillRoundRect(10, height-20, maxWidth, 10, 10, 10);
			g.setColor(160, 160, 160);
			g.fillRoundRect(10, height-20, barWidth, 10, 10, 10);
		}
		
	}
	
	protected class RemoteControlCanvas extends GameCanvas implements Runnable {
		private boolean dirty = false;
		private boolean shown = false;
		private Vector guiElements;
		
		protected RemoteControlCanvas(String name) {
			super(false);
			setTitle(name);
			guiElements = new Vector();
			guiElements.addElement(new TvshowThumb());
			guiElements.addElement(new FileThumb());
			guiElements.addElement(new PlaybackProgress());
		}
		
		public void run() {
			synchronized(this) {
				if(shown) {
					System.out.println("Running");
					if(dirty) {
						dirty = false;
						int height = getHeight();
						int width = getWidth();
						Graphics g = getGraphics();
						HttpApi api = getApi();
						
						drawBackground(width, height);
						
						for(Enumeration e = guiElements.elements(); e.hasMoreElements(); ) {
							GUIElement element = (GUIElement) e.nextElement();
							element.fetch(api, width, height);
							element.paint(g, width, height);
							dirty = dirty || element.getDirty();
						}

						flushGraphics();
					}
					System.out.println("Done");
				} else {
					System.out.println("Invisible");
				}

			}
		}
		
		public void refresh() {
			System.out.println("Refreshing");
			new Thread(this).start();
		}
		
		public synchronized void setValue(String name, String newValue) {
			for(Enumeration e = guiElements.elements(); e.hasMoreElements(); ) {
				GUIElement element = (GUIElement) e.nextElement();
				dirty = element.updateValue(name, newValue) || dirty;
			}
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
				g.setColor(i*40/iterations, i*40/iterations, i*40/iterations);
				g.fillRoundRect(i, i, width-2*i, height-2*i, iterations, iterations);
			}
		}
		
		protected void sizeChanged(int w, int h) {
			System.out.println("sizeChanged");
			synchronized(this) {
				for(Enumeration e = guiElements.elements(); e.hasMoreElements(); ) {
					GUIElement element = (GUIElement) e.nextElement();
					element.sizeChanged(w, h);
				}
				dirty = true;
			}
			super.sizeChanged(w, h);
			refresh();
		}
		
		protected void hideNotify() {
			System.out.println("hideNotify");
			synchronized(this) {
				shown = false;
			}
		}
		
		protected void showNotify() {
			System.out.println("showNotify");
			setupListener();
			synchronized(this) {
				shown = true;
			}
			refresh();
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
