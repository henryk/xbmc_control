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
	
	public static boolean arrayPrefixEqual(byte[] a1, int off1, byte[] a2, int off2, int len)
	{
		if(off1+len > a1.length)
			return false;
		if(off2+len > a2.length)
			return false;
		for(int i=0; i<len; i++)
			if(a1[off1+i] != a2[off2+i])
				return false;
		return true;
	}

	/** Read from is until one of the strings in toMatch is matched. Returns an array of 
	 * two elements: The first element is the data read up to the matcher, the second is the
	 * sequence from toMatch that matched, or {} in case of stream end before complete match
	 * @param is An InputStream to read from
	 * @param toMatch An array of byte arrays with the data to be matched
	 * @return An array of 2 byte arrays. The first item contains the byte sequence that was read,
	 * 	the second is the byte sequence from toMatch that was matched.   
	 */
	public static byte[][] findRead(InputStream is, byte[][] toMatch) throws IOException
	{
		byte[][] result = new byte[2][];
		final int matchcount = toMatch.length;
		final int bufferIncrement = 1024;
		byte buffer[] = new byte[bufferIncrement];
		int matchLen = 0;
		int nonmatchLen = 0;
		
		result[1] = new byte[]{};
		
		int ch, i;
		boolean fullMatchFound = false;
		while( (ch = is.read()) != -1) {
			if(nonmatchLen + matchLen + 1 >= buffer.length) {
				byte[] newBuffer = new byte[buffer.length+bufferIncrement];
				System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
				buffer = newBuffer;
			}
			buffer[ nonmatchLen + (matchLen++) ] = (byte) ch;
			boolean prefixMatchFound = false;
			for(i=0; i<matchcount; i++) {
				if(arrayPrefixEqual(buffer, nonmatchLen, toMatch[i], 0, matchLen)) {
					prefixMatchFound = true;
					if(toMatch[i].length == matchLen) {
						fullMatchFound = true;
						result[0] = new byte[nonmatchLen];
						System.arraycopy(buffer, 0, result[0], 0, nonmatchLen);
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
				nonmatchLen += matchLen;
				matchLen = 0;
			}
		}
		
		if(!fullMatchFound) {
			nonmatchLen += matchLen;
			matchLen = 0;
			result[0] = new byte[nonmatchLen];
			System.arraycopy(buffer, 0, result[0], 0, nonmatchLen);
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
