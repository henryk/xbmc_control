package ch.ploetzli.xbmc.j2me;

import java.util.Vector;

import javax.microedition.midlet.MIDlet;

import ch.ploetzli.xbmc.Logger;

public class MidletLogger extends Logger {
	private Vector entries = new Vector();
	private final static int MAX_ENTRIES = 50;

	public MidletLogger(MIDlet midlet) {
	}

	public synchronized void info(String s) {
		if(entries.size() >= MAX_ENTRIES)
			entries.removeElementAt(0);
		entries.addElement("I "+s+"\n");
		System.out.println(s);
	}
	
	public synchronized void error(String s) {
		if(entries.size() >= MAX_ENTRIES)
			entries.removeElementAt(0);
		entries.addElement("E "+s+"\n");
		System.err.println(s);
	}
	
	public synchronized String[] getCompleteLog() {
		String result[] = new String[entries.size()];
		entries.copyInto(result);
		return result;
	}
}
