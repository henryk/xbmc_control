package ch.ploetzli.xbmc.api;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/* This is a hacked together mDNS resolver that will look specifically for the XBMC web interface.
 * Since J2ME has no explicit multicast support it will send out multiple requests with
 * the "do accept unicast responses" bit set in the hope that the responder will respond
 * to at least one of them with a unicast response. (Typically the responder will send a multicast
 * response which can not be received by J2ME. However, it limits the number of multicast responses
 * and switches to unicast after that.)
 */

public class MdnsDiscoverer {
	private DatagramConnection conn;
	private final byte[] query = new byte[]{
			0x00, 0x00, /* Transaction ID */
			0x00, 0x00, /* Flags */
			0x00, 0x01, /* Queries */
			0x00, 0x00, /* Answer RRs */
			0x00, 0x00, /* Authority RRs */
			0x00, 0x00, /* Additional RRs */
			
			/* Queries */
			0x09, '_', 'x', 'b', 'm', 'c', '-', 'w', 'e', 'b',
				0x04, '_', 't', 'c', 'p',
				0x05, 'l', 'o', 'c', 'a', 'l',
				0x00, /* End of name */
				0x00, 0x0c, /* Type: PTR */
				-128, 0x01, /* Class IN, "QU" question */
		};
	private MdnsNagThread nagThread;
	private MdnsReceiveThread receiveThread;
	private MdnsDiscovererListener listener;
	
	private class MdnsNagThread extends Thread
	{
		/* This thread is responsible for constantly sending multicast
		 * queries in the hope that at least one of them gets a
		 * unicast response.
		 */
		private boolean exit = false;
		
		public void run() 
		{
			while(!exit) {
				try {
					Datagram d = conn.newDatagram(query.length, "datagram://224.0.0.251:5353");
					d.setData(query, 0, query.length);
					conn.send(d);
					Thread.sleep(500);
					d.setData(query, 0, query.length);
					conn.send(d);
					
					Thread.sleep(5000);
				} catch(IOException e) {
					/* Ignore */
				} catch (InterruptedException e) {
					/* Ignore */
				}
			}
		}
	}
	
	private class MdnsReceiveThread extends Thread
	{
		private boolean exit = false;
		public void run()
		{
			final int max = 1200;
			
			try {
				Datagram d = conn.newDatagram(max);
				while(!exit) {
					conn.receive(d);
					
					/* Handle here */
					System.out.println("Have response");
					parseResponse(d);
					
					d.reset();
					d.setLength(max);
				}
			} catch(Exception e) { /* Must be a class that includes InterruptedException and not only IOException */
				/* Ignore and exit */
			}
		}
	}
	
	private void parseResponse(Datagram d)
	{
		
		try {
			d.readShort(); /* Transaction id */
			int flags = d.readUnsignedShort(); /* Flags */
			if( (flags & 0x8000) != 0 && (flags & 0x000f) == 0 ) {
				/* Response, no error */
			} else {
				return;
			}
			d.readShort(); /* Questions */
			int answers = d.readUnsignedShort();
			d.readShort(); /* Authority RRs */
			d.readShort(); /* Additional RRs */
			
			/* TODO Implement rest */
			
			/* For now, just fake one response */
			listener.deviceFound("blacky.local", "192.168.146.110", 8080);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public MdnsDiscoverer(MdnsDiscovererListener listener) throws IOException
	{
		this.listener = listener;
		conn = (DatagramConnection)Connector.open("datagram://:5353");
		this.nagThread = new MdnsNagThread();
		this.receiveThread = new MdnsReceiveThread();
		
		this.receiveThread.start();
		this.nagThread.start();
	}
	
	public void shutdown()
	{
		this.nagThread.exit = true;
		this.receiveThread.exit = true;
		this.receiveThread.interrupt();
	}

}
