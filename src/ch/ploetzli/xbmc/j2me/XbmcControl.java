package ch.ploetzli.xbmc.j2me;
import java.util.Hashtable;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import ch.ploetzli.xbmc.Logger;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.mdns.MdnsDiscoverer;
import ch.ploetzli.xbmc.api.mdns.MdnsDiscovererListener;
import ch.ploetzli.xbmc.j2me.views.MovieGenreView;
import ch.ploetzli.xbmc.j2me.views.MovieTitleView;
import ch.ploetzli.xbmc.j2me.views.MovieYearView;
import ch.ploetzli.xbmc.j2me.views.MusicAlbumView;
import ch.ploetzli.xbmc.j2me.views.MusicArtistView;
import ch.ploetzli.xbmc.j2me.views.MusicGenreView;
import ch.ploetzli.xbmc.j2me.views.TvshowGenreView;
import ch.ploetzli.xbmc.j2me.views.TvshowTitleView;
import ch.ploetzli.xbmc.j2me.views.TvshowYearView;

public class XbmcControl extends MIDlet implements CommandListener, MdnsDiscovererListener {

	private List deviceList;
	private Display display;
	private Command exit;
	private Command connect;
	private MdnsDiscoverer disc = null;
	private HttpApi api;
	
	private Hashtable devices;
	
	public XbmcControl() {
		Logger.overrideLogger(new MidletLogger(this));
		
		this.display = Display.getDisplay(this);
		this.deviceList = new List("Select Device", List.IMPLICIT);
		this.deviceList.setCommandListener(this);
		
		this.exit = new Command("Exit", Command.EXIT, 0x01);
		this.deviceList.addCommand(this.exit);
		this.connect = new Command("Connect!", Command.OK, 0x01);
		
		this.devices = new Hashtable();
		
		this.api = null;
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#startApp()
	 */
	protected void startApp() throws MIDletStateChangeException {
		this.display.setCurrent(this.deviceList);
		try {
			this.disc = new MdnsDiscoverer(this);
			if(disc != null) {
				deviceList.setTicker(new Ticker("Searching for devices ..."));
			}
		} catch(Exception e) {
			Logger.getLogger().error(e);
		}
	}

	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#destroyApp(boolean)
	 */
	protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {}

	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#pauseApp()
	 */
	protected void pauseApp() {}

	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command command, Displayable displayable) {
		if (command == this.exit) {
			synchronized(this) {
				if(this.disc != null)
					this.disc.shutdown();
			}
			this.notifyDestroyed();
		} else if(command == this.connect) {
			new Thread(
					new Runnable() {
						public void run() {
							try {
								doConnect(deviceList.getString(deviceList.getSelectedIndex()));
							} catch(Exception e) { Logger.getLogger().error(e); }
						}
					}).start();
		}
	}
	
	private DatabaseTopMenu topMenu;
	
	public void doConnect(String displayName)
	{
		Object[] data;
		String address;
		int port;
		
		synchronized(this) {
			data = (Object[])this.devices.get(displayName);
			if(data == null) 
				return;
		
			address = (String)data[0];
			port = ((Integer)data[1]).intValue();
		
			if(disc != null) {
				disc.shutdown();
				disc = null;
			}
		}
		
		api = new HttpApi(displayName, address, port);
		
		topMenu = new DatabaseTopMenu(displayName, new SubMenu[]{
				new RemoteControl("Remote"),
				new SubMenu("Movies", new SubMenu[]{
						MovieGenreView.get("Genre"),
						MovieTitleView.get("Title"),
						MovieYearView.get("Year"),
				}),
				new SubMenu("TV Shows", new SubMenu[]{
						TvshowGenreView.get("Genre"),
						TvshowTitleView.get("Title"),
						TvshowYearView.get("Year"),
				}),
				new SubMenu("Music", new SubMenu[]{
						MusicGenreView.get("Genre"),
						MusicArtistView.get("Artist"),
						MusicAlbumView.get("Album"),
				}),
				new DebugView("Debug"),
		});
		
		topMenu.setApi(api);
		topMenu.addCommand(exit);
		topMenu.setCommandListener(this);
		topMenu.setDisplay(display);
		
		display.setCurrent(topMenu.getDisplayable());
	}
	
	public void deviceFound(String name, String address, int port) {
		synchronized(this) {
			String displayName = (name + ":" + port).intern();
			Object data = new Object[]{address, new Integer(port)};
			if(devices.containsKey(displayName)) {
				/* Nothing to do here */
			} else {
				devices.put(displayName, data);
				deviceList.append(displayName, null);
			}

			deviceUpdate();
		}
	}
	
	public void deviceLost(String name, String address, int port) {
		synchronized(this) {
			String displayName = (name + ":" + port).intern();
			if(devices.containsKey(displayName)) {
				devices.remove(displayName);
				for(int i=0; i<deviceList.size(); i++) {
					if(deviceList.getString(i).equals(displayName)) {
						deviceList.delete(i);
						break;
					}
				}
			} else {
				/* Nothing to do here */
			}

			deviceUpdate();
		}
	}
	
	public void deviceUpdate()
	{
		if(devices.isEmpty()) {
			deviceList.removeCommand(connect);
		} else {
			deviceList.addCommand(connect);
			deviceList.setSelectCommand(connect);
		}
	}

	
}
