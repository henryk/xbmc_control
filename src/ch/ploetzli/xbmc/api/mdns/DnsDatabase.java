package ch.ploetzli.xbmc.api.mdns;

import java.util.Enumeration;
import java.util.Vector;

/* 
 * This is a minimal DNS response database that will hold multiple responses
 * per key. The format of the entries is Object[]{int type; String[] key; Vector contents}
 * where the contents Vector contains objects of a DnsRecord subclass.
 */
public class DnsDatabase {
	private Vector database;

	public DnsDatabase() {
		this.database = new Vector();
	}

	public void addRecord(String[] name, DnsRecord data, int ttl)
	{
		/* Check whether this exact record is already in there, if so update its ttd, 
		 * otherwise append it.
		 * Note that the ttd has to be assigned in any case since the passed in data
		 * object will not have its ttd set. */
		long ttd = System.currentTimeMillis() + ttl*1000L;
		int type = data.getType();
		Vector contents = null;
		
		/* Find type, key pair */
		for(Enumeration e = database.elements(); e.hasMoreElements() ;) {
			Object[] entry = (Object[])e.nextElement();
			Integer otherType = (Integer)entry[0];
			if(type != otherType.intValue())
				continue;
			String[] otherName = (String[])entry[1];
			if(!DnsRecord.namesEqual(name, otherName))
				continue;
			contents = (Vector)entry[2];
			break;
		}
		
		if(contents == null) {
			/* Not found, add it */
			contents = new Vector();
			Object entry[] = new Object[]{new Integer(type), name, contents};
			database.addElement(entry);
		}
		
		/* Find if the same record is already known for that type, key pair */
		DnsRecord found = null;
		for(Enumeration e = contents.elements(); e.hasMoreElements(); ) {
			DnsRecord otherRecord = (DnsRecord)e.nextElement();
			if(otherRecord.equals(data)) {
				found = otherRecord;
				break;
			}
		}
		
		if(found == null) {
			/* Not found, add */
			data.timeToDie = ttd;
			contents.addElement(data);
		} else {
			/* Update time to die */
			if(ttd > found.timeToDie) {
				found.timeToDie = ttd;
			}
		}
	}
	
	public void timeoutRecords()
	{
		long currentTime = System.currentTimeMillis();
		
		for(Enumeration e = database.elements(); e.hasMoreElements() ;) {
			Object[] entry = (Object[])e.nextElement();
			Vector contents = (Vector)entry[2];
			for(int i = 0; i<contents.size(); i++) {
				DnsRecord record = (DnsRecord)contents.elementAt(i);
				if(record.timeToDie < currentTime) {
					contents.removeElementAt(i);
					i--;
				}
			}
		}
	}
	
	public DnsRecord[] findRecord(int type, String[] key)
	{
		for(Enumeration e = database.elements(); e.hasMoreElements() ;) {
			Object[] entry = (Object[])e.nextElement();
			Integer otherType = (Integer)entry[0];
			if(type != otherType.intValue())
				continue;
			String[] otherName = (String[])entry[1];
			if(!DnsRecord.namesEqual(key, otherName))
				continue;
			Vector contents = (Vector)entry[2];
			
			DnsRecord[] results = new DnsRecord[contents.size()];
			contents.copyInto(results);
			return results;
		}
		
		return new DnsRecord[]{};
	}
}