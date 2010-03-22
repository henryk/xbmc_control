package ch.ploetzli.xbmc.j2me;

import javax.microedition.midlet.MIDlet;

import ch.ploetzli.xbmc.Logger;

public class MidletLogger extends Logger {
	private MIDlet midlet;

	public MidletLogger(MIDlet midlet) {
		this.midlet = midlet;
	}
}
