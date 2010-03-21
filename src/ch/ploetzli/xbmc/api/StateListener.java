package ch.ploetzli.xbmc.api;

public interface StateListener {

	/**
	 * Called by a StateMonitor object when a state variable has changed. The property
	 * strings are interned and should be safe to compare with ==. The contents of the
	 * property and newValue arguments are determined by the remote HTTP-API and
	 * implementors should ignore all values they don't recognize.
	 * 
	 * This method might be called synchronously from a separate Thread, so implementors
	 * MUST NOT do anything that blocks or takes too long.
	 * 
	 * @param property The name of the property that has changed.
	 * @param newValue The new value of the property, or null if the property was deleted.
	 */
	void valueChanged(String property, String newValue);

	/**
	 * Called after a series of valueChanged() calls to indicate that the transmitted
	 * state is synchronized to the remote state. An implementor that wants to reduce
	 * spurious user interface changes might want to accumulate multiple valueChanged()
	 * calls and only display them after stateSynchronized() has been called.
	 * 
	 * This method might be called synchronously from a separate Thread, so implementors
	 * MUST NOT do anything that blocks or takes too long.
	 */
	void stateSynchronized();
}
