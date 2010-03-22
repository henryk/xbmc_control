package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

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
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().registerListener(this, StateMonitor.INTEREST_PERCENTAGE);
		}
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
	
	private void pause() {
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().unregisterListener(this);
		}
	}
	
	private void unpause() {
		HttpApi api = getApi();
		if(api != null) {
			api.getStateMonitor().registerListener(this, StateMonitor.INTEREST_PERCENTAGE);
		}
	}
	
	public void stateSynchronized() {
		if(canvas == null)
			constructDisplayable();
		if(canvas != null)
			canvas.refresh();
	}

	public void valueChanged(String property, String newValue) {
		if(property.equals("Percentage")) {
			System.out.println("Play progress: " + newValue);
		} else if(property.equals("Thumb")) {
			if(canvas == null)
				constructDisplayable();
			if(canvas != null)
				canvas.setThumbUrl(newValue);
		} else if(property.equals("Show Title")) {
			if(canvas == null)
				constructDisplayable();
			if(canvas != null)
				canvas.setTvshowTitle(newValue);
		}
	}
	
	protected class RemoteControlCanvas extends GameCanvas implements Runnable {
		private Image thumb = null;
		private String thumbUrl = null;
		private boolean thumbDirty = false;
		
		private Image tvshowThumb = null;
		private String tvshowTitle = null;
		private boolean tvshowDirty = false;

		protected RemoteControlCanvas(String name) {
			super(false);
			setTitle(name);
		}
		
		public void run() {
			synchronized(this) {
				if(thumbDirty || tvshowDirty) {
					drawBackground(getWidth(), getHeight());
					
					if(thumbDirty) {
						thumb = null;
						try {
							thumb = ImageFactory.getRemoteImage(getApi(), thumbUrl);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if(thumb != null) {
							Graphics g = getGraphics();
							int height = getHeight();
							int width = getWidth();

							thumb = ImageFactory.scaleImageToFit(thumb, (int)(width*0.4), (int)(height*0.5));

							g.drawImage(thumb, 10, height-10, Graphics.BOTTOM | Graphics.LEFT);
						}
					}
					
					if(tvshowDirty) {
						tvshowThumb = null;
						HttpApi api = getApi();
						if(api != null && tvshowTitle != null) {
							try {
								RecordSetConnection conn = api.queryVideoDatabase("SELECT strPath,c00 FROM tvshowview WHERE c00 = '"+tvshowTitle+"' LIMIT 1");
								String data[] = new String[]{};
								if(conn.hasMoreElements())
									data = (String[]) conn.nextElement();
								if(data.length > 0) {
									String crc = Utils.crc32(data[0]);
									tvshowThumb = ImageFactory.getRemoteImage(api, "special://userdata/Thumbnails/Video/"+ crc.charAt(0) + "/" + crc + ".tbn");
								}

								if(tvshowThumb != null) {
									Graphics g = getGraphics();
									int height = getHeight();
									int width = getWidth();

									tvshowThumb = ImageFactory.scaleImageToFit(tvshowThumb,
											width-20, (int)(height*0.3));

									g.drawImage(tvshowThumb, 10, 10, Graphics.TOP | Graphics.LEFT);
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					tvshowDirty = thumbDirty = false;
					flushGraphics();
				}
			}
		}
		
		public void refresh() {
			new Thread(this).start();
		}
		
		public synchronized void setThumbUrl(String newValue) {
			if( thumbUrl == null && newValue == null)
				return; /* Nothing to do */
			else if( thumbUrl != null || newValue != null || !thumbUrl.equals(newValue)) { 
				System.out.println("Thumb dirty");
				thumbUrl = newValue;
				thumbDirty = true;
			}
		}

		public synchronized void setTvshowTitle(String newValue) {
			if( tvshowTitle == null && newValue == null)
				return; /* Nothing to do */
			else if( tvshowTitle != null || newValue != null || !tvshowTitle.equals(newValue)) {
				System.out.println("Show dirty");
				tvshowTitle = newValue;
				tvshowDirty = true;
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
			super.sizeChanged(w, h);
			refresh();
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
