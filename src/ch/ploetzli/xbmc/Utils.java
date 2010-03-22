package ch.ploetzli.xbmc;

import java.io.IOException;
import java.io.InputStream;

public class Utils {

	public static void debugPrintStringArray(String array[])
	{
		int i;
		for(i=0; i<array.length; i++) 
			Logger.getLogger().info(array[i]);
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
	public static String[] findRead(InputStream is, String[] toMatch, boolean isUtf8) throws IOException
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
		
		if(isUtf8) {
			/* This is a horrible hack to solve the encoding problem, but I couldn't find
			 * a proper way to solve it earlier. In principle one should not treat the
			 * incoming byte stream as a String up to this point but as a byte sequence
			 * (and also convert all the entries of the toMatch array to a byte sequence
			 * before matching) and then only decode into a String at this point. However,
			 * StringBuffer is the most convenient way to incrementally build a buffer and
			 * not worry about memory management, so I'm using that. The downside of course
			 * is that StringBuffer will construct a String and not a byte sequence.
			 */
			result[0] = new String(result[0].getBytes("windows-1252"), "UTF-8");
		}
		
		return result;
	}

	public static String[] findRead(InputStream is, String[] toMatch) throws IOException
	{
		return findRead(is, toMatch, false);
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

	public static boolean stringArraysEqual(String a[], String b[])
	{
		if(a == null && b == null) return true;
		if(a == null && b != null) return false;
		if(a.length != b.length) return false;
		for(int i=0; i<a.length; i++) {
			if(a[i] == null && b[i] == null)
				continue;
			if(a[i] != null && b[i] == null)
				return false;
			if(!a[i].equals(b[i]))
				return false;
		}
		return true;
	}
}
