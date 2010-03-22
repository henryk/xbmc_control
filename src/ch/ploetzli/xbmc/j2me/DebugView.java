package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import ch.ploetzli.xbmc.Logger;

public class DebugView extends SubMenu {
	private Command refresh = new Command("Refresh", Command.ITEM, 5);
	
	public DebugView(String name) {
		super(name);
	}
	
	protected Displayable constructDisplayable() {
		Form form = new Form(name);
		addPrivateCommands(form);
		form.setCommandListener(this);
		return form;
	}
	
	protected void addPrivateCommands(Displayable d) {
		d.addCommand(refresh);
		super.addPrivateCommands(d);
	}
	
	public void commandAction(Command cmd, Displayable d) {
		if(cmd == refresh)
			refresh();
		else
			super.commandAction(cmd, d);
	}
	
	public void refresh() {
		Form form = (Form)getDisplayable();
		form.deleteAll();
		
		Logger l = Logger.getLogger();
		if(l instanceof MidletLogger) {
			MidletLogger logger = (MidletLogger) l;
			String logEntries[] = logger.getCompleteLog();
			for(int i = 0; i<logEntries.length; i++) {
				form.append(logEntries[i]);
			}
		}
	}
}
