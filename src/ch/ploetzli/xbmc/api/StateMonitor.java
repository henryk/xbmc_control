package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
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
	private Hashtable properties = new Hashtable();
	
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
		String status[] = new String[]{};
		try {
			status = httpApi.getCurrentlyPlaying(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(int i=0; i<status.length; i++) {
			int pos = status[i].indexOf(':');
			/* intern() the property value since it's probably going to be used a lot */
			String property = status[i].substring(0, pos).intern();
			String value = status[i].substring(pos+1);
			
			if(property.equals("Changed")) {
				/* Meaningless since it's shared by all HTTP-API clients */
				continue;
			}
			
			String oldValue = (String)properties.get(property);
			if(oldValue == null || !oldValue.equals(value)) {
				valueChanged(property, value);
				properties.put(property, value);
			}
			
		}
		
		stateSynchronized();
	}

	private void stateSynchronized() {
		for(Enumeration e = listeners.elements(); e.hasMoreElements(); ) 
			((StateListener)e.nextElement()).stateSynchronized();
		
	}

	private void valueChanged(String property, String value) {
		for(Enumeration e = listeners.elements(); e.hasMoreElements(); ) 
			((StateListener)e.nextElement()).valueChanged(property, value);
	}

	public synchronized void registerListener(StateListener l) {
		if(!listeners.contains(l)) {
			listeners.addElement(l);
			/* Send the current state */
			for(Enumeration e = properties.keys(); e.hasMoreElements(); ) {
				String property = (String) e.nextElement();
				l.valueChanged(property, (String)properties.get(property));
			}
			l.stateSynchronized();	
		}
	}
	
	public synchronized void unregisterListener(StateListener l) {
		listeners.removeElement(l);
	}
}
