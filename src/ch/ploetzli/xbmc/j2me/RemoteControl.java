package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.game.GameCanvas;

public class RemoteControl extends DatabaseSubMenu {
	
	public RemoteControl(String name) {
		super(name);
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
