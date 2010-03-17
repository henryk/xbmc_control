package ch.ploetzli.xbmc.api.mdns;

public interface MdnsDiscovererListener {
	public void deviceFound(String name, String address, int port);

	public void deviceLost(String name, String address, int port);
}
