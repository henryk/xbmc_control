package ch.ploetzli.xbmc.j2me;
import java.io.IOException;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import ch.ploetzli.xbmc.Utils;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

public class XbmcPoc extends MIDlet implements CommandListener {

	private Form list;
	private Display display;
	private Command exit;
	private Command fetch;
	private HttpApi api;
	
	private int width;
	
	public XbmcPoc() {
		this.display = Display.getDisplay(this);
		this.list = new Form("Test");
		this.list.setCommandListener(this);
		
		this.exit = new Command("Exit", Command.EXIT, 0x01);
		this.list.addCommand(this.exit);
		this.fetch = new Command("Fetch!", Command.ITEM, 0x01);
		this.list.addCommand(this.fetch);
		
		this.width = this.list.getWidth() - 10;
			
		this.api = new HttpApi();
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#startApp()
	 */
	protected void startApp() throws MIDletStateChangeException {
		this.display.setCurrent(this.list);
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
			this.notifyDestroyed();
		} else if(command == this.fetch) {
			new Thread(
					new Runnable() {
						public void run() {
							doFetch();
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
					
					this.list.append(img);
				} else if(o instanceof Exception) {
					((Exception)o).printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			this.list.append(e.toString());
		}
	}
	
}
