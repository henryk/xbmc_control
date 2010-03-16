package ch.ploetzli.xbmc;

import java.io.IOException;
import java.io.InputStream;

public class Utils {

	public static void debugPrintStringArray(String array[])
	{
		int i;
		for(i=0; i<array.length; i++) 
			System.err.println(array[i]);
	}
	
	public static String URIEscape(String input)
	{
		StringBuffer output = new StringBuffer();
		byte in[] = input.getBytes();
		for(int i=0; i<in.length; i++) {
			if(in[i] >= 'a' && in[i] <= 'z' ) {
				output.append((char)in[i]);
			} else if(in[i] >= 'A' && in[i] <= 'Z' ) {
				output.append((char)in[i]);
			} else if(in[i] >= '0' && in[i] <= '9' ) {
				output.append((char)in[i]);
			} else if(in[i] == '(' || in[i] == ')' ) {
				output.append((char)in[i]);
			} else {
				output.append("%");
				output.append(Integer.toHexString(in[i]));
			}
		}
		return output.toString();
	}

	public static String readFull(InputStream is) throws IOException
	{
		 StringBuffer b = new StringBuffer();
		 int ch;
		 while ((ch = is.read()) != -1) {
	         b.append((char)ch);
	     }
		 return b.toString();
	}

	public static void assertRead(InputStream is, char c) throws IOException
	{
		int ch = is.read();
		if(ch != c) {
			throw new IOException("Got character " + ch + ", was expecting " + (int)c);
		}
	}

	public static void assertRead(InputStream is, String s) throws IOException
	{
		int i;
		for(i=0; i<s.length(); i++) {
			assertRead(is, s.charAt(i));
		}
	}

	/* Read from is until one of the strings in toMatch is matched. Returns an array of 
	 * two elements: The first element is the data read up to the matcher, the second is the
	 * string from toMatch that matched, or "" in case of stream end before complete match   
	 */
	public static String[] findRead(InputStream is, String[] toMatch) throws IOException
	{
		String[] result = new String[2];
		int matchcount = toMatch.length;
		StringBuffer matching = new StringBuffer();
		StringBuffer nonmatching = new StringBuffer();
		
		result[1] = "";
		
		int ch, i;
		boolean fullMatchFound = false;
		while( (ch = is.read()) != -1) {
			matching.append((char)ch);
			String m = matching.toString();
			boolean prefixMatchFound = false;
			for(i=0; i<matchcount; i++) {
				if(toMatch[i].startsWith(m)) {
					prefixMatchFound = true;
					if(toMatch[i].equals(m)) {
						fullMatchFound = true;
						result[0] = nonmatching.toString();
						result[1] = toMatch[i];
					}
					break;
				}
			}
			
			if(fullMatchFound)
				break;
			
			if(!prefixMatchFound) {
				/* Move the matching buffer to the end of the nonmatching buffer and start again with a 
				 * clean matching buffer
				 */
				nonmatching.append(m);
				matching.delete(0, matching.length());
			}
		}
		
		if(!fullMatchFound) {
			nonmatching.append(matching.toString());
			result[0] = nonmatching.toString();
		}
		
		return result;
	}

	/* This is a CRC-32 routine that works with the way the xbmc crc32 thumbnail generation
	 * works.
	 */
	public static String crc32(String input)
	{
		long CRC=0xffffffff;
		input = input.toLowerCase();
		char[] data = input.toCharArray();
		for (int j=0; j<data.length; j++) {
			int c = data[j];
			CRC ^= c << 24;
			for(int i = 0; i<8; i++) {
				if( (CRC & 0x80000000L) != 0) {
					CRC = (CRC << 1) ^ 0x04C11DB7;
				} else{
					CRC <<= 1;
				}
			}
		}
		CRC &= 0xffffffffL;
		
		String r = Long.toString(CRC, 16);
		while(r.length() < 8)
			r = "0".concat(r);
		return r;
	}
}
