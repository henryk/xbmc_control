package ch.ploetzli.xbmc.api;

public interface StateListener {

	void stateSynchronized();

	void valueChanged(String property, String newValue);

}
