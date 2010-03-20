package ch.ploetzli.xbmc.j2me;

/**
 * This class cooperates with and needs an instance of DatabaseTopMenu in the parent
 * chain in order to find a reference to the database, represented by an HttpApi
 * object. As such this class and all subclasses are XBMC HTTP-API specific, while
 * SubMenu is generic and reusable.
 * 
 * @author henryk
 *
 */
public abstract class DatabaseSubMenu extends SubMenu {

	public DatabaseSubMenu(String name, SubMenu[] subMenus) {
		super(name, subMenus);
	}

	public DatabaseSubMenu(String name) {
		super(name);
	}

	/**
	 * Follows the parent chain to find an instance of DatabaseTopMenu and returns it.
	 * @return
	 */
	protected DatabaseTopMenu getDatabaseTopMenu()
	{
		SubMenu parent = this;
		while( (parent = parent.getParent()) != null) {
			if(parent instanceof DatabaseSubMenu) {
				DatabaseTopMenu topMenu = ((DatabaseSubMenu)parent).getDatabaseTopMenu();
				return topMenu;
			}
		}
		return null;
	}

}
