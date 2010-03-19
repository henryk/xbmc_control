package ch.ploetzli.xbmc.api.mdns;

public abstract class DnsRecord {
	final static int TYPE_PTR = 0x0c;
	final static int TYPE_SRV = 0x21;
	final static int TYPE_A = 0x01;
	
	long timeToDie;
	
	abstract int getType();
	
	public int hashCode()
	{
		/* Not nice or efficient but at least correct */
		return getType();
	}

	public static String flattenName(String name[])
	{
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<name.length; i++) {
			if(i!=0) 
				buf.append(".");
			buf.append(name[i]);
		}
		return buf.toString();
	}

}
