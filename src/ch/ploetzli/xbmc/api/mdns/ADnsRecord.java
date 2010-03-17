package ch.ploetzli.xbmc.api.mdns;

public class ADnsRecord extends DnsRecord {

	public String data;

	public ADnsRecord(String data) {
		this.data = data;
	}

	int getType() {
		return TYPE_A;
	}
	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof ADnsRecord))
			return false;
		return ((ADnsRecord)obj).data.equals(data);
	}

}
