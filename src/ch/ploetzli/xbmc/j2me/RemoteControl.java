package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.game.GameCanvas;

public class RemoteControl extends SubMenu {
	
	public RemoteControl(String name) {
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
			if(parent instanceof DatabaseTopMenu) {
				DatabaseTopMenu topMenu = ((DatabaseTopMenu)parent);
				return topMenu;
			}
		}
		return null;
	}

	protected Displayable constructDisplayable() {
		Displayable canvas = new RemoteControlCanvas(name);
		canvas.addCommand(backCommand);
		canvas.setCommandListener(this);
		return canvas;
	}
	
	protected class RemoteControlCanvas extends GameCanvas {

		protected RemoteControlCanvas(String name) {
			super(false);
			setTitle(name);
		}
		
	}
}
