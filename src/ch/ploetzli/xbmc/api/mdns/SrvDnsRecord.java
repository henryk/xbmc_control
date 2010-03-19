package ch.ploetzli.xbmc.api.mdns;

import ch.ploetzli.xbmc.Utils;

public class SrvDnsRecord extends DnsRecord {

	public int priority;
	public int weight;
	public int port;
	public String[] target;

	public SrvDnsRecord(int priority, int weight, int port, String target[])
	{
		this.priority = priority;
		this.weight = weight;
		this.port = port;
		this.target = target;
	}
	
	int getType() {
		return TYPE_SRV;
	}

	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof SrvDnsRecord))
			return false;
		SrvDnsRecord that = (SrvDnsRecord)obj;
		
		if(this.priority != that.priority) return false;
		if(this.weight != that.weight) return false;
		if(this.port != that.port) return false;
		
		return Utils.stringArraysEqual(this.target, that.target);
	}

}
