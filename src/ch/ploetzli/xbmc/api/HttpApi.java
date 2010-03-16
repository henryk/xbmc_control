package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.*;

import ch.ploetzli.xbmc.Utils;

public class HttpApi {
	private String baseurl;
	
	public HttpApi()
	{
		baseurl = "http://192.168.146.110:8080//xbmcCmds/xbmcHttp";
	}
	
	protected HttpConnection openCommandConnection(String cmd) throws IOException
	{
		HttpConnection conn = (HttpConnection)Connector.open(baseurl + "?command=" + Utils.URIEscape(cmd));
		
		int rc = conn.getResponseCode();
        if (rc != HttpConnection.HTTP_OK) {
            throw new IOException("HTTP response code: " + rc);
        }

		return conn;
	}
	
	public String[] simpleCommand(String cmd, boolean stripSpaces) throws IOException
	{
		HttpConnection conn = openCommandConnection(cmd);
		Vector result = new Vector();
		InputStream is = conn.openInputStream();
		
		/* All responses start with <html>, so match that and bail in case of mismatch */
		Utils.assertRead(is, "<html>");
		
		/* The individual items are separated by <li>, 
		 * input ends with </html> */
		final String[] tokens = new String[]{"<li>", "</html"};
		try {
			boolean keepGoing = true;
			boolean first = true;
			boolean hadone = false;
			do {
				String r[] = Utils.findRead(is, tokens);
				if(r[1].equals(tokens[0])) {
					hadone = true;
					if(first) {
						/* Ignore the text before the first <li> */
						first = false;
					} else {
						if(stripSpaces)
							result.addElement(r[0].trim());
						else
							result.addElement(r[0]);
					}
				} else {
					if(hadone) {
						if(stripSpaces)
							result.addElement(r[0].trim());
						else
							result.addElement(r[0]);
					}
					keepGoing = false;
				}
			} while(keepGoing);
			
		} finally {
			conn.close();
		}
		
		String resultArray[] = new String[result.size()];
		result.copyInto(resultArray);
		
		return resultArray;
	}
	
	public String[] simpleCommand(String cmd) throws IOException
	{
		return simpleCommand(cmd, false);
	}
	
	public RecordSetConnection databaseCommand(String cmd) throws IOException
	{
		HttpConnection conn = openCommandConnection(cmd);
		InputStream is = conn.openInputStream();
		
		/* All responses start with <html>, so match that and bail in case of mismatch */
		Utils.assertRead(is, "<html>");
		
		return new RecordSetConnection(is);
	}
	
	public RecordSetConnection queryVideoDatabase(String query) throws IOException
	{
		return databaseCommand("QueryVideoDatabase("+query+")");
	}
	
	public RecordSetConnection queryMusicDatabase(String query) throws IOException
	{
		return databaseCommand("QueryMusicDatabase("+query+")");
	}
	
	public String[] getCurrentPlaylist() throws IOException
	{
		return simpleCommand("GetCurrentPlaylist()", true);
	}
	
	public String[] getGUIDescription() throws IOException
	{
		return simpleCommand("GetGUIDescription()", true);
	}
	
	public String[] getGUIStatus() throws IOException
	{
		return simpleCommand("GetGUIStatus()", true);
	}
	
	public byte[] fileDownload(String url) throws IOException
	{
		HttpConnection conn = openCommandConnection("FileDownload("+url+";bare)");
		InputStream is = conn.openInputStream();
		String data;
		try {
			data = Utils.readFull(is);
		} finally {
			conn.close();
		}
		if(data.trim().equals("")) {
			return new byte[]{};
		}
		return org.kobjects.base64.Base64.decode(data);
	}
	
}
