package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

public class LibraryList extends List implements CommandListener
{
	private HttpApi api;
	private final String labelMovies = "Movies";
	private final String labelTvShows = "TV Shows";
	private final String labelGenres = "Genres";
	private final String labelTitle = "Title";
	
	private Command selectCommand;
	private Command backCommand;
	private CommandListener upperListener;
	
	private final int STATE_UNINITIALIZED = -1;
	private final int STATE_TOPLEVEL = 0;
	private final int STATE_MOVIES_TOPLEVEL = 1;
	private final int STATE_TVSHOWS_TOPLEVEL = 2;
	private final int STATE_MOVIES_TITLE = 3;
	private int state = -1;
	private int level = 0;
	private int selectionIndexes[] = new int[10];
	
	private FillingThread fillingThread = null;

	public LibraryList(HttpApi api)
	{
		super(api.getName() + " Library", IMPLICIT);
		this.api = api;
		
		this.setState(0);
		
		this.selectCommand = new Command("Select", Command.OK, 15);
		this.backCommand = new Command("Back", Command.BACK, 10);
		
		/* Adding a command will only take up a softkey. The command
		 * set with setSelectCommand usually already has a hardkey
		 * assigned
		 */
		//this.addCommand(this.selectCommand);
		//this.setSelectCommand(this.selectCommand);
		
		super.setCommandListener(this);
	}
	
	private void setState(int newState)
	{
		/* undo current state */
		if(fillingThread != null) {
			fillingThread.shutdown();
		}
		
		if(state == STATE_UNINITIALIZED) {
			/* Nothing to do */
		} else if(state == STATE_TOPLEVEL) {
			this.deleteAll();
		} else if(state == STATE_MOVIES_TOPLEVEL) {
			this.removeCommand(backCommand);
			this.deleteAll();
		} else if(state == STATE_TVSHOWS_TOPLEVEL) {
			this.removeCommand(backCommand);
			this.deleteAll();
		} else if(state == STATE_MOVIES_TITLE) {
			this.removeCommand(backCommand);
			this.deleteAll();
		}
		
		/* set new state */
		if(newState == STATE_TOPLEVEL) {
			this.setTitle(api.getName() + " Library");
			this.append(labelMovies, null);
			this.append(labelTvShows, null);
		} else if(newState == STATE_MOVIES_TOPLEVEL) {
			this.setTitle(api.getName() + " Library - Movies");
			this.append(labelGenres, null);
			this.append(labelTitle, null);
			this.addCommand(backCommand);
		} else if(newState == STATE_TVSHOWS_TOPLEVEL) {
			this.setTitle(api.getName() + " Library - TV Shows");
			this.append(labelGenres, null);
			this.append(labelTitle, null);
			this.addCommand(backCommand);
		} else if(newState == STATE_MOVIES_TITLE) {
			this.setTitle(api.getName() + " Movies - Title");
			this.addCommand(backCommand);
			this.fillingThread = new FillingThread(FillingThread.ROLE_MOVIE_BY_TITLE);
		}
		state = newState;
	}
	
	private class FillingThread extends Thread
	{
		static final int ROLE_MOVIE_BY_TITLE = 0;
		int role;
		boolean exit = false;
		
		FillingThread(int role)
		{
			super();
			this.role = role;
			this.start();
		}
		
		public void run()
		{
			try {
				if(role == ROLE_MOVIE_BY_TITLE)
					fillMovieByTitle();
			} catch(Exception e) {
				e.printStackTrace();
				/* Ignore and end thread */
			}
		}
		
		void shutdown()
		{
			exit = true;
		}
		
		protected void fillMovieByTitle() throws IOException {
			RecordSetConnection connection = null;
			try {
				connection = api.queryVideoDatabase("select idMovie, c00 from movieview order by c00");
				while(connection.hasMoreElements()) {
					String[] row = (String[])connection.nextElement();
					if(exit)
						break;
					if(row.length > 1)
						append(row[1], null);
				}
			} finally {
				if(connection != null) connection.shutdown();
			}
		}
	}

	private void selected(String label)
	{
		selectionIndexes[level] = this.getSelectedIndex();
		if(state == STATE_UNINITIALIZED) {
			/* Nothing to do */
		} else if(state == STATE_TOPLEVEL) {
			if(label == labelMovies) {
				setState(STATE_MOVIES_TOPLEVEL);
			} else if(label == labelTvShows) {
				setState(STATE_TVSHOWS_TOPLEVEL);
			}
			level++;
		} else if(state == STATE_MOVIES_TOPLEVEL) {
			if(label == labelTitle) {
				setState(STATE_MOVIES_TITLE);
			}
			level++;
		}
		if(this.size() > 0)
			this.setSelectedIndex(0, true);
	}
	
	private void back()
	{
		selectionIndexes[level] = this.getSelectedIndex();
		if(state == STATE_UNINITIALIZED) {
			/* Nothing to do */
		} else if(state == STATE_TOPLEVEL) {
			/* Nothing to do */
		} else if(state == STATE_MOVIES_TOPLEVEL) {
			setState(STATE_TOPLEVEL);
			level--;
		} else if(state == STATE_TVSHOWS_TOPLEVEL) {
			setState(STATE_TOPLEVEL);
			level--;
		} else if(state == STATE_MOVIES_TITLE) {
			setState(STATE_MOVIES_TOPLEVEL);
			level--;
		}
		if(this.size() > selectionIndexes[level])
			this.setSelectedIndex(selectionIndexes[level], true);
	}

	/* This class will always handle its own actions first and 
	 * then chain up to the listener set with setCommandListener
	 */
	public void commandAction(Command cmd, Displayable display) {
		if(cmd == selectCommand || cmd == List.SELECT_COMMAND) {
			selected( this.getString(this.getSelectedIndex()) );
		} else if(cmd == backCommand) {
			back();
		} else if(upperListener != null) {
			upperListener.commandAction(cmd, display);
		}
	}

	public void setCommandListener(CommandListener l) {
		upperListener = l;
	}

}
