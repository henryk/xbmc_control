package ch.ploetzli.xbmc.api.mdns;

import ch.ploetzli.xbmc.Utils;

public class PtrDnsRecord extends DnsRecord {

	public String[] data;

	int getType() {
		return TYPE_PTR;
	}
	
	public PtrDnsRecord(String[] data)
	{
		this.data = data;
	}

	public boolean equals(Object obj)
	{
		if(!(obj instanceof PtrDnsRecord))
			return false;
		return Utils.stringArraysEqual( ((PtrDnsRecord)obj).data, data);
	}

}
