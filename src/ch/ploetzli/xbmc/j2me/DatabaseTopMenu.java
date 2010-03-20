package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Displayable;

import ch.ploetzli.xbmc.api.HttpApi;

public class DatabaseTopMenu extends SubMenu {
	protected int databaseEpoch = 0;
	
	public DatabaseTopMenu(String name, SubMenu[] subMenus) {
		super(name, subMenus);
	}

	private HttpApi api;

	
	public void setApi(HttpApi api)
	{
		this.api = api;
	}
	
	public HttpApi getApi()
	{
		return api;
	}
	
	public synchronized int getDatabaseEpoch()
	{
		return databaseEpoch;
	}
	
	/**
	 * Invalidate all children's database caches by changing the
	 * databaseEpoch value.
	 */
	public synchronized void invalidateCache()
	{
		databaseEpoch++;
	}

	protected Displayable constructDisplayable() {
		Displayable d = super.constructDisplayable();
		/* This is the top level menu, remove the back command */
		d.removeCommand(backCommand);
		return d;
	}
	
	
}
