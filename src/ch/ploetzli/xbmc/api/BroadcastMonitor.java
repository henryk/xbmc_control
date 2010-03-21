package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Connector;

public class BroadcastMonitor extends Thread
{
	private DatagramConnection conn;
	private Hashtable listeners = new Hashtable();
	boolean exit = false;

	public BroadcastMonitor(int port)
	{
		super();
		try {
			this.conn = (DatagramConnection)Connector.open("datagram://:"+port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			int notificationLevel = ((Integer)listeners.get(listener)).intValue();
			if(notificationLevel >= level)
				listener.broadcastReceived(address, name, value, level);
		}
	}
	
	public synchronized void addListener(BroadcastListener listener, int notificationLevel) {
		listeners.put(listener, new Integer(notificationLevel));
	}
	
	public synchronized void removeListener(BroadcastListener listener) {
		listeners.remove(listener);
	}

	public void shutdown() {
		exit = true;
		this.interrupt();
	}
}
