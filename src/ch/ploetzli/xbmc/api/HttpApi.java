package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.*;

import ch.ploetzli.xbmc.Utils;

public class HttpApi {
	private String baseurl;
	private String name;
	private StateMonitor stateMonitor;
	private BroadcastMonitor broadcastMonitor;
	
	public HttpApi(String displayName, String address, int port)
	{
		baseurl = "http://"+address+":"+port+"/xbmcCmds/xbmcHttp";
		name = displayName;
		
		/* State monitor needs the broadcast monitor to be initialized first */
		broadcastMonitor = new BroadcastMonitor(this);
		stateMonitor = new StateMonitor(this);
	}
	
	public String getName()
	{
		return name;
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
	
	public RecordSetConnection databaseCommand(String cmd, boolean isUtf8) throws IOException
	{
		HttpConnection conn = openCommandConnection(cmd);
		InputStream is = conn.openInputStream();
		
		/* All responses start with <html>, so match that and bail in case of mismatch */
		Utils.assertRead(is, "<html>");
		
		return new RecordSetConnection(is, isUtf8);
	}
	
	public RecordSetConnection queryVideoDatabase(String query) throws IOException
	{
		return databaseCommand("QueryVideoDatabase("+query+")", true);
	}
	
	public RecordSetConnection queryMusicDatabase(String query) throws IOException
	{
		return databaseCommand("QueryMusicDatabase("+query+")", true);
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
	
	public String[] getBroadcast() throws IOException
	{
		return simpleCommand("GetBroadcast()", true);
	}
	
	public void setBroadcast(int setting, int broadcastPort) throws IOException 
	{
		simpleCommand("SetBroadcast("+setting+";"+broadcastPort+")");
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

	public void sendKey(int buttoncode) throws IOException {
		simpleCommand("SendKey("+buttoncode+")");
	}

	public StateMonitor getStateMonitor() {
		return stateMonitor;
	}
	
	public BroadcastMonitor getBroadcastMonitor() {
		return broadcastMonitor;
	}

	public String[] getCurrentlyPlaying(boolean extended) throws IOException {
		return simpleCommand("GetCurrentlyPlaying(,,,"+extended+",)", true);
	}

}
