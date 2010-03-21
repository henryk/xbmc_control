package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * There is no notification of playback progress, so this thread
 * will periodically poll the status.
 * @author henryk
 *
 */
public class StateMonitor extends Thread implements BroadcastListener {
	private boolean exit = false;
	private int maxInterestLevel = 0;
	private final static int maxPollDelay = 60000;
	private long pollDelay = maxPollDelay;
	private HttpApi api;
	private Hashtable listeners = new Hashtable();
	private Hashtable properties = new Hashtable();
	private Object cookie = new Object();
	
	/**
	 * The listener is only interested in basic information such as
	 * play state and current file name.
	 */
	public final static int INTEREST_BASIC = 1;
	
	/**
	 * The listener is furthermore interested in more or less accurate
	 * information about the play progress as a percentage of file length.
	 */
	public final static int INTEREST_PERCENTAGE = 2;
	
	/**
	 * The listener is furthermore interested in changes to the current
	 * play progress as a time value, with approximately one second precision.
	 */
	public final static int INTEREST_TIME = 3;
	
	public StateMonitor(HttpApi api) {
		super();
		this.api = api;
		start();
	}

	public void run() {
		api.getBroadcastMonitor().addListener(this, 1);
		while(!exit) {
			try {
				synchronized(this) {
					this.wait(pollDelay);
					if(maxInterestLevel > 0 && !exit)
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
			status = api.getCurrentlyPlaying(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String oldKeys[] = new String[properties.size()];
		{ int i = 0;
			for(Enumeration e = properties.keys(); e.hasMoreElements(); )
				oldKeys[i++] = (String)e.nextElement();
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
			
			for(int j = 0; j<oldKeys.length; j++)
				if(oldKeys[j] != null && oldKeys[j].equals(property))
					oldKeys[j] = null;
		}
		
		/* Send notification about all properties that are not included in the current status */
		for(int i = 0; i<oldKeys.length; i++) {
			if(oldKeys[i] != null) {
				valueChanged(oldKeys[i], null);
				properties.remove(oldKeys[i]);
			}
		}
		
		stateSynchronized();
	}

	private void stateSynchronized() {
		for(Enumeration e = listeners.keys(); e.hasMoreElements(); ) 
			((StateListener)e.nextElement()).stateSynchronized();
		
	}

	private void valueChanged(String property, String value) {
		System.out.println("Value changed "+property+" = "+value);
		for(Enumeration e = listeners.keys(); e.hasMoreElements(); ) 
			((StateListener)e.nextElement()).valueChanged(property, value);
	}

	private synchronized void setMaxInterestLevel(int interestLevel) {
		maxInterestLevel = interestLevel;
		if(maxInterestLevel == 0) {
			/* No one cares */
			pollDelay = maxPollDelay;
		} else if(maxInterestLevel == INTEREST_BASIC) {
			/* The broadcast monitor will wake us */
			pollDelay = maxPollDelay;
		} else if(maxInterestLevel == INTEREST_PERCENTAGE) {
			/* FIXME Do something smart with timing */
			pollDelay = 10000;
		} else if(maxInterestLevel == INTEREST_TIME) {
			pollDelay = 1000;
		}
		this.notify();
	}
	
	private synchronized void schedulePoll() {
		System.out.println("Schedule this!");
		this.notify();
	}

	/**
	 * Register a StateListener to be notified of state changes by this monitor object.
	 * When a listener is registered it will receive a series of callbacks to
	 * valueChanged() and stateSynchronized() to set up the initial state. The listener
	 * should assume an empty initial state, even if the same listener is repeatedly
	 * registered and unregistered to the same monitor.
	 * @param listener The listener to be registered. No action will be taken if the
	 * 	listener is already registered.
	 * @param interestLevel Specifies the interest level of this listener. Must be one
	 * 	of the INTEREST_* constants. The monitor uses this information to optimize the
	 * 	polling interval or even completely disable polling (if broadcast information
	 * 	is available).
	 */
	public synchronized void registerListener(StateListener listener, int interestLevel) {
		if(!listeners.containsKey(listener)) {
			listeners.put(listener, new Integer(interestLevel));
			if(maxInterestLevel < interestLevel)
				setMaxInterestLevel(interestLevel);
			/* Send the current state */
			for(Enumeration e = properties.keys(); e.hasMoreElements(); ) {
				String property = (String) e.nextElement();
				listener.valueChanged(property, (String)properties.get(property));
			}
			listener.stateSynchronized();	
		}
	}
	
	/**
	 * Unregister a StateListener. 
	 * @param listener The listener to be unregistered. No action will be taken
	 * 	if the listener is not registered to this monitor object.
	 */
	public synchronized void unregisterListener(StateListener listener) {
		if(listeners.containsKey(listener)) {
			listeners.remove(listener);
			int m = 0;
			for(Enumeration e = listeners.elements(); e.hasMoreElements(); ) {
				int interestLevel = ((Integer)e.nextElement()).intValue();
				if(m < interestLevel)
					m = interestLevel;
			}
			setMaxInterestLevel(m);
		}
	}

	public void broadcastReceived(String source, String name, String data, int level) {
		schedulePoll();
	}
	
	/* There is some extensive weirdness going on that seems to prevent Thread objects
	 * from being put into a Hashtable. The hashCode() seems to be changing between the
	 * initial put() and the subsequent get(). As a workaround, borrow the hashCode from
	 * a private object.
	 */
	public int hashCode() {
		return cookie.hashCode();
	}
}
