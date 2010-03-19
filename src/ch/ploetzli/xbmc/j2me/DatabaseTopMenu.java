package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Displayable;

import ch.ploetzli.xbmc.api.HttpApi;

public class DatabaseTopMenu extends SubMenu {
	
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

	protected Displayable constructDisplayable() {
		Displayable d = super.constructDisplayable();
		/* This is the top level menu, remove the back command */
		d.removeCommand(backCommand);
		return d;
	}
	
	
}