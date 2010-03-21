package ch.ploetzli.xbmc.api;

public interface BroadcastListener {
	public void broadcastReceived(String source, String name, String data, int level);
}
