package ch.ploetzli.xbmc.j2me;
import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import ch.ploetzli.xbmc.Utils;
import ch.ploetzli.xbmc.api.BroadcastMonitor;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;
import ch.ploetzli.xbmc.api.mdns.MdnsDiscoverer;
import ch.ploetzli.xbmc.api.mdns.MdnsDiscovererListener;

public class XbmcPoc extends MIDlet implements CommandListener, MdnsDiscovererListener {

	private Form seriesList;
	private List deviceList;
	private Display display;
	private Command exit;
	private Command fetch;
	private Command connect;
	private MdnsDiscoverer disc = null;
	private HttpApi api;
	
	private int width;
	private Hashtable devices;
	private BroadcastMonitor broadcastMonitor;
	
	public XbmcPoc() {
		this.display = Display.getDisplay(this);
		this.deviceList = new List("Select Device", List.IMPLICIT);
		this.deviceList.setCommandListener(this);
		this.seriesList = new Form("Test");
		this.seriesList.setCommandListener(this);
		
		this.exit = new Command("Exit", Command.EXIT, 0x01);
		this.seriesList.addCommand(this.exit);
		this.deviceList.addCommand(this.exit);
		this.fetch = new Command("Fetch!", Command.ITEM, 0x01);
		this.seriesList.addCommand(this.fetch);
		this.connect = new Command("Connect!", Command.OK, 0x01);
		
		this.width = this.seriesList.getWidth() - 10;
		
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
		} catch(IOException e) {
			System.out.println(e);
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
			if(this.disc != null)
				this.disc.shutdown();
			if(this.broadcastMonitor != null)
				this.broadcastMonitor.shutdown();
			this.notifyDestroyed();
		} else if(command == this.fetch) {
			new Thread(
					new Runnable() {
						public void run() {
							doFetch();
						}
					}).start();
		} else if(command == this.connect) {
			new Thread(
					new Runnable() {
						public void run() {
							doConnect(deviceList.getString(deviceList.getSelectedIndex()));
						}
					}).start();
		}
	}
	
	/* Image scaling from http://willperone.net/Code/codescaling.php */
	private static Image scaleImage(Image original, int newWidth, int newHeight)
	{        
	    int[] rawInput = new int[original.getHeight() * original.getWidth()];
	    original.getRGB(rawInput, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
	    
	    int[] rawOutput = new int[newWidth*newHeight];        

	    // YD compensates for the x loop by subtracting the width back out
	    int YD = (original.getHeight() / newHeight) * original.getWidth() - original.getWidth(); 
	    int YR = original.getHeight() % newHeight;
	    int XD = original.getWidth() / newWidth;
	    int XR = original.getWidth() % newWidth;        
	    int outOffset= 0;
	    int inOffset=  0;
	    
	    for (int y= newHeight, YE= 0; y > 0; y--) {            
	        for (int x= newWidth, XE= 0; x > 0; x--) {
	            rawOutput[outOffset++]= rawInput[inOffset];
	            inOffset+=XD;
	            XE+=XR;
	            if (XE >= newWidth) {
	                XE-= newWidth;
	                inOffset++;
	            }
	        }            
	        inOffset+= YD;
	        YE+= YR;
	        if (YE >= newHeight) {
	            YE -= newHeight;     
	            inOffset+=original.getWidth();
	        }
	    }               
	    return Image.createRGBImage(rawOutput, newWidth, newHeight, false);        
	}
	
	private static Image scaleImage(Image original, int newWidth)
	{
		return scaleImage(original, newWidth, (newWidth*original.getHeight())/original.getWidth());
	}

	public void doFetch()
	{
		if(api == null)
			return;
		System.out.println("Good boy");
		try {
			RecordSetConnection conn = api.queryVideoDatabase("select strPath,c00 from tvshowview order by c00");
			while(conn.hasMoreElements()) {
				Object o = conn.nextElement();
				if(o instanceof String[]) {
					String path = ((String[])o)[0];
					String name = ((String[])o)[1];
					String crc = Utils.crc32(path);
					
					System.out.println(Utils.crc32(path) + " " + name);
					
					byte imageData[] = api.fileDownload("special://userdata/Thumbnails/Video/"+ crc.charAt(0) + "/" + crc + ".tbn");
					Image img = Image.createImage(imageData, 0, imageData.length);
					img = scaleImage(img, width);
					
					this.seriesList.append(img);
				} else if(o instanceof Exception) {
					((Exception)o).printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			this.seriesList.append(e.toString());
		}
	}
	
	private DatabaseTopMenu topMenu;
	
	public void doConnect(String displayName)
	{
		Object[] data = (Object[])this.devices.get(displayName);
		String address = (String)data[0];
		int port = ((Integer)data[1]).intValue();
		
		disc.shutdown();
		disc = null;
		
		api = new HttpApi(displayName, address, port);
		seriesList.setTitle(displayName);
		
		topMenu = new DatabaseTopMenu(displayName, new SubMenu[]{
				new RemoteControl("Remote"),
				new SubMenu("Library", new SubMenu[]{
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
				}),
		});
		
		topMenu.setApi(api);
		topMenu.addCommand(exit);
		topMenu.setCommandListener(this);
		topMenu.setDisplay(display);
		
		display.setCurrent(topMenu.getDisplayable());
	}
	
	public void deviceFound(String name, String address, int port) {
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
	
	public void deviceLost(String name, String address, int port) {
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
