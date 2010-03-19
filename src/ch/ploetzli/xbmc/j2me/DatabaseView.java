package ch.ploetzli.xbmc.j2me;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Ticker;

import ch.ploetzli.xbmc.Utils;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

public class DatabaseView extends SubMenu {
	protected static Ticker ticker = new Ticker("Loading ...");
	
	protected String keyColumn;
	protected String[] dataColumns;
	protected String table;
	protected String orderClause;
	protected String groupClause;
	protected String whereClause;
	protected String[] cacheValidator = null;
	protected Vector cache = new Vector();
	protected FillingThread fillingThread = null;
	
	public DatabaseView(String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause, String whereClause)
	{
		super(name, new SubMenu[]{});
		this.keyColumn = keyColumn;
		this.dataColumns = dataColumns;
		this.table = table;
		this.orderClause = orderClause;
		this.groupClause = groupClause;
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
		/* TODO: Implement caching logic here */
		return make(name, keyRow, dataRows, table, orderClause, groupClause, whereClause);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause)
	{
		return DatabaseView.get(name, keyRow, dataRows, table, orderClause, groupClause, null);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table, String orderClause)
	{
		return DatabaseView.get(name, keyRow, dataRows, table, orderClause, null, null);
	}

	public static DatabaseView get(String name, String keyRow, String[] dataRows, String table)
	{
		return DatabaseView.get(name, keyRow, dataRows, table, null, null, null);
	}
	
	/** Factory method to return newly made object. <b>Must</b> be overriden by all subclasses */
	protected static DatabaseView make(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause, String whereClause)
	{
		System.out.println("Making DatabaseView");
		return new DatabaseView(name, keyRow, dataRows, table, orderClause, groupClause, whereClause);
	}

	/**
	 * Return our grandfathers' HttpApi.
	 * Follows the parent chain to find an instance of DatabaseTopMenu and calls
	 * getApi() on it.
	 * @return
	 */
	protected HttpApi getApi()
	{
		SubMenu parent = this;
		while( (parent = parent.getParent()) != null) {
			if(parent instanceof DatabaseTopMenu) {
				HttpApi api = ((DatabaseTopMenu)parent).getApi();
				if(api != null)
					return api;
			}
		}
		return null;
	}

	/**
	 * Spawn a new Thread to update this menu's contents.
	 * @see ch.ploetzli.xbmc.j2me.SubMenu#refresh()
	 */
	public void refresh()
	{
		synchronized(this) {
			if(fillingThread != null) {
				fillingThread.shutdown();
				fillingThread = null;
			}

			Displayable d = getDisplayable();
			if(d != null && d.getTicker() != ticker) {
				d.setTicker(ticker);
			}
			
			fillingThread = new FillingThread();
		}
	}
	
	protected void refreshFinished()
	{
		synchronized(this) {
			Displayable d = getDisplayable();
			if(d != null && d.getTicker() == ticker)
				d.setTicker(null);
		}
	}

	/**
	 * Check if this DatabaseView's cache is still valid.
	 * The cache is protected by a validator that is sum() and count() over the
	 * key column. This should be guaranteed to catch any deletion and insertion
	 * of rows, since keys are strictly monotonic.
	 * @param api An HttpApi object
	 * @param storeNewValidator If true then the current validator will be stored
	 * 	so that later calls to this function will return true unless further
	 * 	modifications take place.
	 * @return false if no key column is set; otherwise queries the database and
	 * 	returns false if the cache validator does not match the stored value
	 */
	public boolean cacheValid(HttpApi api, boolean storeNewValidator) throws IOException
	{
		if(keyColumn == null)
			return false;
		if(!storeNewValidator && cacheValidator == null) {
			/* No cache yet */
			return false;
		}
		
		String newValidator[] = fetchCacheValidator(api);
		
		if(newValidator == null || cacheValidator == null) {
			if(storeNewValidator)
				cacheValidator = newValidator;
			return false;
		} else if(!Utils.stringArraysEqual(newValidator,cacheValidator)) {
			if(storeNewValidator)
				cacheValidator = newValidator;
			return false;
		} else {
			return true;
		}
	}
	
	public String[] fetchCacheValidator(HttpApi api) throws IOException
	{
		RecordSetConnection conn = null;
		String newValidator[] = null;
		try {
			/* TODO Maybe include where clause? */
			conn = api.queryVideoDatabase("select count("+keyColumn+"), sum("+keyColumn+") from "+table);
			if(conn.hasMoreElements()) {
				Object o = conn.nextElement();
				if(o instanceof String[]) {
					newValidator = (String[])o;
				}
			}
		} finally {
			if(conn != null)
				conn.shutdown();
		}
		return newValidator;
	}

	
	private class FillingThread extends Thread
	{
		boolean exit = false;
		
		FillingThread()
		{
			super();
			this.start();
		}
		
		public void run()
		{
			try {
				HttpApi api = getApi();
				if(api != null) {
					String[] newValidator = fetchCacheValidator(api);
					if(newValidator == null 
							|| cacheValidator == null 
							|| !Utils.stringArraysEqual(newValidator, cacheValidator)) {
						fetchData(api);
						
						if(!exit) 
							cacheValidator = newValidator;
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
				/* Ignore and end thread */
			} finally {
				refreshFinished();
			}
		}
		
		private void fetchData(HttpApi api) throws IOException
		{
			RecordSetConnection conn = null;
			try {
				conn = api.queryVideoDatabase(constructQuery());
				int i=0;
				while(conn.hasMoreElements()) {
					Object o = conn.nextElement();
					if(o instanceof String[]) {
						haveRecord(i++, (String[])o);
					}
					if(exit)
						break;
				}
				if(!exit) {
					haveRecordCount(i);
				}
			} finally {
				if(conn != null)
					conn.shutdown();

			}
		}

		void shutdown()
		{
			exit = true;
		}
	}


	protected String constructQuery() {
		StringBuffer query = new StringBuffer();
		
		query.append("SELECT ");
		
		if(keyColumn != null) {
			query.append(keyColumn+", ");
		}
		
		if(dataColumns != null) {
			for(int i=0; i<dataColumns.length; i++) {
				if(i != 0)
					query.append(", ");
				query.append(dataColumns[i]);
			}
		}
		
		query.append(" FROM ");
		query.append(table);
		
		if(whereClause != null) {
			query.append(" WHERE ");
			query.append(whereClause);
		}
		
		if(groupClause != null) {
			query.append(" GROUP BY ");
			query.append(groupClause);
		}
		
		if(orderClause != null) {
			query.append(" ORDER BY ");
			query.append(orderClause);
		}
		
		return query.toString();
	}

	/**
	 * This update method is incrementally called from the FillingThread
	 * to update our knowledge of rows. 
	 * @param i Index of the row
	 * @param data Data for the row, array of [keyColumn] + dataColumns
	 */
	protected void haveRecord(int i, String[] data) {
		if(cache.size() > i) {
			if(!Utils.stringArraysEqual(data, (String[])cache.elementAt(i))) {
				updateRow(i, data);
				cache.setElementAt(data, i);
			}
		} else if(cache.size() == i) {
			appendRow(i, data);
			cache.addElement(data);
		} else {
			/* haveRecord will and must be called in order. Per the above line the cache will
			 * grow with each record, so hitting this alternative means that i has incremented
			 * by 2 between calls.
			 */
			System.err.println("BUG: haveRecord("+i+"), but only "+cache.size()+" objects in cache");
		}
		
	}
	
	/**
	 * This update method is finally called from the FillingThread
	 * to update our knowledge of rows. 
	 * @param i Total number of rows in the database that match our query
	 */
	protected void haveRecordCount(int len) {
		if(cache.size() == len) {
			/* Nothing to do */
		} else if(cache.size() < len) {
			System.err.println("BUG: haveRecordCount("+len+"), but only "+cache.size()+" objects in cache");
		} else while(cache.size() > len) {
			removeRow(cache.size()-1);
			cache.removeElementAt(cache.size()-1);
		}
	}
	
	/**
	 * Update GUI with new database row. The default implementation simply uses
	 * 	the first non-key column as a label, but subclasses might want to override
	 * @param index
	 * @param data
	 */
	protected void appendRow(int index, String[] data)
	{
		String label = "No label";
		if(data.length > 0) {
			label = data[0];
			if(keyColumn != null && data.length > 1) {
				label = data[1];
			}
		}
		((List)getDisplayable()).append(label, null);
	}

	/**
	 * Update GUI with changed database row. The default implementation simply uses
	 * 	the first non-key column as a label, but subclasses might want to override
	 * @param index
	 * @param data
	 */
	protected void updateRow(int index, String[] data)
	{
		String label = "No label";
		if(data.length > 0) {
			label = data[0];
			if(keyColumn != null && data.length > 1) {
				label = data[1];
			}
		}
		((List)getDisplayable()).set(index, label, null);
	}

	/**
	 * Update GUI with removed row.
	 * @param index
	 */
	protected void removeRow(int index)
	{
		((List)getDisplayable()).delete(index);
	}
}
