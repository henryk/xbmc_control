package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

public class DatabaseView extends SubMenu {
	protected String keyRow;
	protected String[] dataRows;
	protected String table;
	protected String orderClause;
	protected String whereClause;
	
	public DatabaseView(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause, String whereClause)
	{
		super(name);
		this.keyRow = keyRow;
		this.dataRows = dataRows;
		this.table = table;
		this.orderClause = orderClause;
		this.whereClause = whereClause;
	}

	public DatabaseView(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause)
	{
		this(name, keyRow, dataRows, table, orderClause, groupClause, null);
	}
	
	public DatabaseView(String name, String keyRow, String[] dataRows, String table, String orderClause)
	{
		this(name, keyRow, dataRows, table, orderClause, null, null);
	}

	public DatabaseView(String name, String keyRow, String[] dataRows, String table)
	{
		this(name, keyRow, dataRows, table, null, null, null);
	}
	
	/* Factory methods to return cached objects */
	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause, String whereClause)
	{
		return new DatabaseView(name, keyRow, dataRows, table, orderClause, groupClause, whereClause);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause)
	{
		return new DatabaseView(name, keyRow, dataRows, table, orderClause, groupClause);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table, String orderClause)
	{
		return DatabaseView.get(name, keyRow, dataRows, table, orderClause);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table)
	{
		return DatabaseView.get(name, keyRow, dataRows, table);
	}
	
	/**
	 * Return our grandfathers' HttpApi.
	 * Follows the parent chain to find an instance of DatabaseTopMenu and calls
	 * getApi() on it.
	 * @return
	 */
	protected HttpApi getApi()
	{
		SubMenu parent;
		while( (parent = getParent()) != null) {
			if(parent instanceof DatabaseTopMenu) {
				HttpApi api = ((DatabaseTopMenu)parent).getApi();
				if(api != null)
					return api;
			}
		}
		return null;
	}

	private FillingThread fillingThread = null;

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
				HttpApi api = getApi();
				if(api != null) {
					connection = api.queryVideoDatabase("select idMovie, c00 from movieview order by c00");
					while(connection.hasMoreElements()) {
						String[] row = (String[])connection.nextElement();
						if(exit)
							break;
						if(row.length > 1)
							//append(row[1], null);
							;
					}
				}
			} finally {
				if(connection != null) connection.shutdown();
			}
		}
	}
}
