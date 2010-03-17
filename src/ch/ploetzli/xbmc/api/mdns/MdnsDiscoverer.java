package ch.ploetzli.xbmc.api.mdns;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/* This is a hacked together mDNS resolver that will look specifically for the XBMC web interface.
 * Since J2ME has no explicit multicast support we will use the defined legacy procedure: Send the
 * request from a port that is not 5353, so that the responders will fall back to unicast responses
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
		/* This thread is responsible for repeatedly sending multicast
		 * queries in order to detect devices that come online after our
		 * first query.
		 */
		private boolean exit = false;
		
		public void run() 
		{
			while(!exit) {
				try {
					Datagram d = conn.newDatagram(query.length);
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
					parseResponse(new PositionDatagram(d));
					
					d.reset();
					d.setLength(max);
				}
			} catch(Exception e) { /* Must be a class that includes InterruptedException and not only IOException */
				/* Ignore and exit */
			}
		}
	}
	
	private void parseResponse(PositionDatagram d)
	{
		try {
			d.readShort(); /* Transaction id */
			int flags = d.readUnsignedShort(); /* Flags */
			if( (flags & 0x8000) != 0 && (flags & 0x000f) == 0 ) {
				/* Response, no error */
			} else {
				return;
			}
			int questions = d.readUnsignedShort(); /* Questions */
			int answers = d.readUnsignedShort();
			int authorities = d.readUnsignedShort(); /* Authority RRs */
			int additional = d.readUnsignedShort(); /* Additional RRs */
			
			if(authorities > 0 || additional > 0) {
				/* Can't parse that yet */
				return;
			}
			
			/* Skip all the questions, we know what we asked for */
			for(int i=0; i<questions; i++) {
				readName(d, new Vector(), d.getPosition()); /* Read and discard the name */
				d.readUnsignedShort(); /* Type */
				d.readUnsignedShort(); /* Class */
			}
			
			for(int i=0; i<answers; i++) {
				String name[] = readName(d);
				System.out.println(flattenName(name));
				int type = d.readUnsignedShort(); /* Type */
				int clas = d.readUnsignedShort(); /* Class */
				d.readInt(); /* TTL */
				int length = d.readUnsignedShort();
				if(type == 0x0c && (clas & 0x7fff) == 0x01) {
					/* PTR IN */
					readName(d, new Vector(), d.getPosition());
				} else {
					/* Just ignore */
					d.skipBytes(length);
				}
			}
			
			/* For now, just fake one response */
			listener.deviceFound("blacky.local", "192.168.146.110", 8080);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void readName(PositionDatagram d, Vector results, int offset) throws IOException
	{
		int labelLength;
		int savedLength = d.getLength();
		
		/* (Possibly recursive) handling of compressed names:
		 * Jump to offset, read the name, append parts to results vector,
		 * if reading a pointer: recursively call with new offset
		 */
		d.reset();
		d.setLength(savedLength);
		d.skipBytes(offset);
		
		while( (labelLength = d.readUnsignedByte()) != 0 ) {
			if( (labelLength & 0xc0) == 0xc0) {
				/* Special casing for a compressed name. */
				int pointer = d.readUnsignedByte();
				pointer = pointer | ((labelLength & 0x3F) << 8);
				offset = d.getPosition();
				readName(d, results, pointer);
				d.reset();
				d.setLength(savedLength);
				d.skipBytes(offset);
				break; /* End the loop, a pointer is always the end of the label list */
			}
			byte b[] = new byte[labelLength];
			d.readFully(b);
			results.addElement(new String(b));
		}
	}
	
	private String[] readName(PositionDatagram d) throws IOException
	{
		Vector v = new Vector();
		readName(d, v, d.getPosition());
		String result[] = new String[v.size()];
		v.copyInto(result);
		return result;
	}
	
	private String flattenName(String name[])
	{
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<name.length; i++) {
			if(i!=0) 
				buf.append(".");
			buf.append(name[i]);
		}
		return buf.toString();
	}
	
	public MdnsDiscoverer(MdnsDiscovererListener listener) throws IOException
	{
		this.listener = listener;
		conn = (DatagramConnection)Connector.open("datagram://224.0.0.251:5353");
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
