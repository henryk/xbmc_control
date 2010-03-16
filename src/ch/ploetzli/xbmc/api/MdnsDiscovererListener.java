package ch.ploetzli.xbmc.api;

public interface MdnsDiscovererListener {
	public void deviceFound(String name, String address, int port);
}
