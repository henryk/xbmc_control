package ch.ploetzli.xbmc.j2me;

import java.util.Vector;

import javax.microedition.midlet.MIDlet;

import ch.ploetzli.xbmc.Logger;

public class MidletLogger extends Logger {
	private MIDlet midlet;
	private Vector entries = new Vector();

	public MidletLogger(MIDlet midlet) {
		this.midlet = midlet;
	}

	public synchronized void info(String s) {
		entries.addElement("I "+s+"\n");
		System.out.println(s);
	}
	
	public synchronized void error(String s) {
		entries.addElement("E "+s+"\n");
		System.err.println(s);
	}
	
	public synchronized String[] getCompleteLog() {
		String result[] = new String[entries.size()];
		entries.copyInto(result);
		return result;
	}
}
