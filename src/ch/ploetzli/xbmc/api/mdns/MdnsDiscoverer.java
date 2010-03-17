package ch.ploetzli.xbmc.api.mdns;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
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
	private final static byte[] query = new byte[]{
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
	private final static String[] serviceName = new String[]{"_xbmc-web", "_tcp", "local"};
	private MdnsNagThread nagThread;
	private MdnsReceiveThread receiveThread;
	private MdnsDiscovererListener listener;
	
	private DnsDatabase database;
	private MdnsUpdateThread updateThread;

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
					
					synchronized(updateThread) {
						synchronized(database) {
							parseResponse(new PositionDatagram(d));
						}
					
						updateThread.notify();
					}
					
					d.reset();
					d.setLength(max);
				}
			} catch(Exception e) { /* Must be a class that includes InterruptedException and not only IOException */
				/* Ignore and exit */
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

				/* Skip all the questions, we don't care what we asked for and will take
				 * all answers at face value. */
				for(int i=0; i<questions; i++) {
					readName(d, null, d.getPosition()); /* Read and discard the name */
					d.readUnsignedShort(); /* Type */
					d.readUnsignedShort(); /* Class */
				}

				for(int i=0; i<answers; i++) {
					String name[] = readName(d);
					int type = d.readUnsignedShort(); /* Type */
					int clas = d.readUnsignedShort(); /* Class */
					int ttl = d.readInt(); /* TTL */
					int length = d.readUnsignedShort();

					if((clas & 0x7fff) != 0x01) {
						/* Class is not IN, we can't parse that, skip it */
						d.skipBytes(length);
						continue;
					}

					if(type == 0x0c) {
						/* PTR IN */
						String[] data = readName(d);
						database.addRecord(name, new PtrDnsRecord(data), ttl);
					} else if (type == 0x01) {
						/* A IN */
						StringBuffer buf = new StringBuffer();
						for(int j=0; j<4; j++) {
							if(j!=0) buf.append(".");
							buf.append(Integer.toString(d.readUnsignedByte()));
						}
						database.addRecord(name, new ADnsRecord(buf.toString()), ttl);
					} else if (type == 0x21) {
						/* SRV IN */
						int priority = d.readUnsignedShort();
						int weight = d.readUnsignedShort();
						int port = d.readUnsignedShort();
						String target[] = readName(d);
						database.addRecord(name, new SrvDnsRecord(priority, weight, port, target), ttl);
					} else {
						/* Just ignore */
						d.skipBytes(length);
					}
				}
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
				if(results != null)
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
	}
	
	private class MdnsUpdateThread extends Thread
	{
		public boolean exit = false;
		private Hashtable devices = new Hashtable();
		private Vector newDevices = new Vector();
		private Vector lostDevices = new Vector();
		
		public void run()
		{
			while(!exit) {
				try {
					synchronized(this) {
						/* Wake up every 1s or after each packet is received to update our
						 * view of the service landscape
						 */
						this.wait(1000);
						
						synchronized(database) {
							database.timeoutRecords();
							updateDeviceView();
						}
						
						notifyListeners();
					}
				} catch(Exception e) {
					/* Ignore */
				}
			}
		}

		private void updateDeviceView() {
			/* Make a new view of the device landscape by following the XBMC PTR records
			 * to SRV records and finally to A records. Then calculate the difference between
			 * the new view and the old view and store in newDevices and lostDevices respectively.
			 */
			Hashtable newView = new Hashtable();
			
			DnsRecord[] ptrResults = database.findRecord(DnsRecord.TYPE_PTR, serviceName);
			for(int i = 0; i<ptrResults.length; i++) {
				PtrDnsRecord ptrRecord = (PtrDnsRecord)ptrResults[i];
				DnsRecord[] srvResults = database.findRecord(DnsRecord.TYPE_SRV, ptrRecord.data);
				for(int j = 0; j<srvResults.length; j++) {
					/* In principle multiple SRV records should be evaluated according to their
					 * priority. 
					 */
					SrvDnsRecord srvRecord = (SrvDnsRecord)srvResults[j];
					DnsRecord[] aResults = database.findRecord(DnsRecord.TYPE_A, srvRecord.target);
					if(aResults.length == 0) {
						continue;
					} else {
						/* Simply use the first result */
						ADnsRecord aRecord = (ADnsRecord)aResults[0];
						String address = aRecord.data;
						String name = DnsRecord.flattenName(srvRecord.target);
						int port = srvRecord.port;
						
						newView.put(name+";"+address+";"+port, new Object[]{name, address, new Integer(port)});
					}
				}
			}
			
			/* Keys are assumed to be unique since fields are separated by ; and this can't be 
			 * found within address or port
			 */
			for(Enumeration e = newView.keys(); e.hasMoreElements(); ) {
				String key = (String)e.nextElement();
				if(!devices.containsKey(key)) {
					newDevices.addElement(newView.get(key));
				}
			}
			for(Enumeration e = devices.keys(); e.hasMoreElements(); ) {
				String key = (String)e.nextElement();
				if(!newView.containsKey(key)) {
					lostDevices.addElement(devices.get(key));
				}
			}
			devices = newView;
		}
		
		private void notifyListeners() {
			for(Enumeration e = newDevices.elements(); e.hasMoreElements(); ) {
				Object[] device = (Object[])e.nextElement();
				listener.deviceFound((String)device[0], (String)device[1], ((Integer)device[2]).intValue());
			}
			for(Enumeration e = lostDevices.elements(); e.hasMoreElements(); ) {
				Object[] device = (Object[])e.nextElement();
				listener.deviceLost((String)device[0], (String)device[1], ((Integer)device[2]).intValue());
			}
			newDevices.removeAllElements();
			lostDevices.removeAllElements();
		}
	}
	
	public MdnsDiscoverer(MdnsDiscovererListener listener) throws IOException
	{
		this.listener = listener;
		this.database = new DnsDatabase();
		conn = (DatagramConnection)Connector.open("datagram://224.0.0.251:5353");
		this.nagThread = new MdnsNagThread();
		this.updateThread = new MdnsUpdateThread();
		this.receiveThread = new MdnsReceiveThread();
		
		this.updateThread.start();
		this.receiveThread.start();
		this.nagThread.start();
	}
	
	public void shutdown()
	{
		this.nagThread.exit = true;
		this.receiveThread.exit = true;
		this.receiveThread.interrupt();
		this.updateThread.exit = true;
		this.updateThread.interrupt();
	}

}
