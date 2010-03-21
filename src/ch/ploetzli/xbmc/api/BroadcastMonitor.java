package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Connector;

public class BroadcastMonitor extends Thread
{
	private DatagramConnection conn = null;
	private Hashtable listeners = new Hashtable();
	boolean exit = false;
	private HttpApi api;

	public BroadcastMonitor(HttpApi api)
	{
		super();
		this.api = api;
		int port = enableBroadcast(0);
		try {
			if(port != -1)
				conn = (DatagramConnection)Connector.open("datagram://:"+port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(conn != null)
			start();
	}
	
	public void run()
	{
		final int max = 1200;
		
		try {
			Datagram d = conn.newDatagram(max);
			while(!exit) {
				conn.receive(d);
				
				processMessage(d);
				
				d.reset();
				d.setLength(max);
			}
		} catch(Exception e) { /* Must be a class that includes InterruptedException and not only IOException */
			/* Ignore and exit */
			e.printStackTrace();
		}
	}
	
	protected void processMessage(Datagram d)
	{
		String data = new String(d.getData(), 0, d.getLength());
		System.out.println("Broadcast received from "+ d.getAddress() +": " + data);
		
		int start = data.indexOf("<b>");
		int stop = data.indexOf("</b>");
		
		if(start == -1 || stop == -1 || start > stop) /* Malformed: no <b>message</b> */
			return;
		
		start += 3; /* Skip over <b> */
		int colon = data.indexOf(':', start);
		if(colon >= stop) /* Malformed: no name:value */
			return;
		/* colon == -1 is allowed as a special case: name with no value */
		
		int semicolon = data.lastIndexOf(';', stop);
		if(semicolon == -1 || semicolon < colon) /* Malformed: no name:value;level */
			return;
		
		String name=null, value=null;
		int level=0;
		
		try {
			level = Integer.parseInt(data.substring(semicolon+1, stop));
		} catch(NumberFormatException e) {
			/* Ignore, keep level at 0 */
		}
		
		if(colon == -1) {
			name=data.substring(start, semicolon);
		} else {
			name=data.substring(start, colon);
			value=data.substring(colon+1, semicolon);
		}
		
		broadcastReceived(d.getAddress(), name, value, level);
	}

	private synchronized void broadcastReceived(String address, String name, String value, int level) {
		for(Enumeration e = listeners.keys(); e.hasMoreElements(); ) {
			BroadcastListener listener = (BroadcastListener) e.nextElement();
			Integer o = (Integer) listeners.get(listener);
			int notificationLevel = o.intValue();
			if(notificationLevel >= level)
				listener.broadcastReceived(address, name, value, level);
		}
	}
	
	public int enableBroadcast(int notificationLevel) {
		try {
			String broadcast[] = api.getBroadcast();
			if(broadcast.length > 0 && !(broadcast[0].startsWith("Error"))) {
				int pos = broadcast[0].indexOf(';');
				if(pos != -1) {
					int setting = Integer.parseInt(broadcast[0].substring(0, pos));
					int broadcastPort = Integer.parseInt(broadcast[0].substring(pos+1));
					if(setting < notificationLevel) {
						api.setBroadcast(1, broadcastPort);
					}
					return broadcastPort;
				}
			}
		} catch(IOException e) {
			/* Ignore, but return -1 to signify failure */
			System.err.println(e);
			e.printStackTrace();
		}
		return -1;
	}
	
	public synchronized void addListener(BroadcastListener listener, int notificationLevel) {
		listeners.put(listener, new Integer(notificationLevel));
		enableBroadcast(notificationLevel);
	}
	
	public synchronized void removeListener(BroadcastListener listener) {
		listeners.remove(listener);
	}

	public void shutdown() {
		exit = true;
		this.interrupt();
	}
}
