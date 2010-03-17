package ch.ploetzli.xbmc.api;

import java.io.IOException;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Connector;

public class BroadcastReceiver extends Thread
{
	private DatagramConnection conn;
	boolean exit = false;

	public BroadcastReceiver(int port)
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
				byte[] data = d.getData();
				System.out.println("Broadcast received: " + new String(data,0,d.getLength()));
				
				d.reset();
				d.setLength(max);
			}
		} catch(Exception e) { /* Must be a class that includes InterruptedException and not only IOException */
			/* Ignore and exit */
		}
	}

	public void shutdown() {
		exit = true;
		this.interrupt();
	}
}
