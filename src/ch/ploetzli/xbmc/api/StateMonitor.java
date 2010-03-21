package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Vector;

/**
 * There is no notification of playback progress, so this thread
 * will periodically poll the status.
 * @author henryk
 *
 */
public class StateMonitor extends Thread {
	boolean exit = false;
	private HttpApi httpApi;
	private Vector listeners = new Vector();
	
	public StateMonitor(HttpApi httpApi) {
		super();
		this.httpApi = httpApi;
	}

	public void run() {
		while(!exit) {
			try {
				Thread.sleep(1000);
				synchronized(this) {
					if(listeners.size() > 0)
						pollStatus();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void pollStatus() {
		try {
			String[] status = httpApi.getCurrentlyPlaying(true);
			for(int i=0; i<status.length; i++) {
				System.out.println(status[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void registerListener(StateListener l) {
		if(!listeners.contains(l))
			listeners.addElement(l);
	}
	
	public synchronized void unregisterListener(StateListener l) {
		listeners.removeElement(l);
	}
}
