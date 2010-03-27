package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import ch.ploetzli.xbmc.Logger;

/**
 * There is no notification of playback progress, so this thread
 * will periodically poll the status.
 * 
 * The policy should be something like this (FIXME: not yet implemented)
 * All polling actions will take place in a dedicated thread and therefore
 * be synchronized with each other. The thread will guard a minimum time of
 * 1s between pollings. If polling is requested before 1s since the last
 * polling has passed it will be delayed (and potentially: accumulated).
 * There is no queue, so there can always only be one deferred polling 
 * action scheduled.
 * 
 * Trying to parse useful information from the broadcasts is hopeless, so
 * they will only be taken as a hint to schedule a polling action (subject
 * to rate limiting of course). The interest levels are as follows:
 * 
 * Level 0 (i.e. no listener connected): No periodic polling. No broadcast
 * 	induced polling. Upon exit from level 0 into any other level one
 * 	polling action is to be scheduled.
 * Level 1: Periodic polling every 60s as well as in response to broadcasts.
 * Level 2: Periodic polling every 10s as well as in response to broadcasts.
 * 	The monitor may evaluate polling results and in the presence of a
 * 	Duration field modulate the polling frequency between 1s and 60s to
 * 	match one hundredth of the duration.
 * Level 3: Periodic polling every 1s as well as in response to broadcasts.
 * 
 * For levels 2 and 3 a pause mode is defined: If a PlayStatus field is
 *  present and indicates paused playback, or Filename, Duration and Time
 *  fields are absent (indicating stopped playback), the monitor will enter
 *  pause mode and reduce its polling intervals to 60s for level 2 and 10s
 *  for level 3. Interval polling or broadcast initiated polling may lead to
 *  the pause mode being exited.
 * 
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
	private Object cookie;
	
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
		if(cookie == null)
			cookie = new Object();
	}

	public void run() {
		try {
			api.getBroadcastMonitor().addListener(this, 1);
			while(!exit) {
				try {
					synchronized(this) {
						this.wait(pollDelay);
						if(maxInterestLevel > 0 && !exit)
							pollStatus();
					}
				} catch (InterruptedException e) {
					Logger.getLogger().info(e);
				}
			}
		} catch(Exception e) { Logger.getLogger().error(e); }
	}
	
	public synchronized void pollStatus() {
		String status[] = new String[]{};
		try {
			status = api.getCurrentlyPlaying(true);
		} catch (IOException e) {
			Logger.getLogger().info(e);
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
		//System.out.println("Value changed "+property+" = "+value);
		for(Enumeration e = listeners.keys(); e.hasMoreElements(); ) 
			((StateListener)e.nextElement()).valueChanged(property, value);
	}

	private synchronized void setMaxInterestLevel(int interestLevel) {
		Logger.getLogger().info("New max interest level is "+ interestLevel);
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
		this.notify();
	}

	/**
	 * Register a StateListener to be notified of state changes by this monitor object.
	 * When a listener is registered it will receive a series of callbacks to
	 * valueChanged() and stateSynchronized() to set up the initial state. The listener
	 * should assume an empty initial state, even if the same listener is repeatedly
	 * registered and unregistered to the same monitor.
	 * @param listener The listener to be registered. If the listener was already
	 *  registered with a different interestLevel, the interestLevel will be adjusted.
	 * @param interestLevel Specifies the interest level of this listener. Must be one
	 * 	of the INTEREST_* constants. The monitor uses this information to optimize the
	 * 	polling interval or even completely disable polling (if broadcast information
	 * 	is available).
	 */
	public synchronized void registerListener(StateListener listener, int interestLevel) {
		Object oldVal = listeners.put(listener, new Integer(interestLevel));
		if(oldVal == null) {
			if(maxInterestLevel < interestLevel)
				setMaxInterestLevel(interestLevel);
			/* Send the current state */
			for(Enumeration e = properties.keys(); e.hasMoreElements(); ) {
				String property = (String) e.nextElement();
				listener.valueChanged(property, (String)properties.get(property));
			}
			listener.stateSynchronized();	
		} else {
			if(((Integer)oldVal).intValue() != interestLevel) {
				setMaxInterestLevel(calculateMaxInterestLevel());
			}
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
			setMaxInterestLevel(calculateMaxInterestLevel());
		}
	}

	/**
	 * @return The maximum interest level registered by any listener.
	 */
	private synchronized int calculateMaxInterestLevel() {
		int level = 0;
		for(Enumeration e = listeners.elements(); e.hasMoreElements(); ) {
			int interestLevel = ((Integer)e.nextElement()).intValue();
			if(level < interestLevel)
				level = interestLevel;
		}
		return level;
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
		if(cookie == null) {
			/* Extra super fun: The super() constructor on Nokia series 60
			 * seems to call hashCode(), before cookie is set.
			 */
			cookie = new Object();
		}
		return cookie.hashCode();
	}
}
