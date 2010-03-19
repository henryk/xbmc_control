package ch.ploetzli.xbmc.j2me;

import java.io.IOException;
import java.util.Hashtable;
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
	
	/**
	 * Cache of constructed objects to be used by get
	 */
	protected static Hashtable objectCache = new Hashtable();
	/**
	 * LRU list of keys from the objectCache member, to ensure
	 * the limit of objectCacheMax objects in the cache.
	 * The vector contains the keys into objectCache in ascending order
	 * of least-recent-usedness, e.g. the last element will be the most
	 * recently returned and the first object will be the least recently
	 * returned and next candidate for dropping.
	 */
	protected static Vector objectCacheKeys = new Vector();
	/**
	 * Keep at most this many objects cached in objectCache.
	 * When residency reaches this number the oldest object, as determined
	 * by objectCacheKeys gets removed before a new one is inserted.
	 */
	protected final static int objectCacheMax = 35;
	
	/**
	 * Default constructor, necessary for construction through Class.newInstance();
	 * Never use directly, instead use get(...) which is the effective constructor
	 * for this class and its subclasses.
	 */
	public DatabaseView() { super("No name", new SubMenu[]{}); }
	
	/** This is the effective constructor, active when an instance is requested through get */
	protected static DatabaseView get(Class c, String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause, String whereClause)
	{
		String cacheKey = (c.getName()+";"+name+";"+constructQuery(keyColumn, dataColumns, table, orderClause, groupClause, whereClause)).intern();
		DatabaseView v = null;
		
		synchronized(objectCache) {
			if(objectCache.containsKey(cacheKey)) {
				/* Object already in cache, update LRU list, return cached object */
				objectCacheKeys.removeElement(cacheKey);
				objectCacheKeys.addElement(cacheKey);
				v = (DatabaseView)objectCache.get(cacheKey);
			} else {
				/* Must construct new object */
				try {
					v = (DatabaseView)c.newInstance();
				} catch(Exception e) {
					/* Might as well crash and burn if anything goes wrong here. */
				}
				v.setArguments(name, keyColumn, dataColumns, table, orderClause, groupClause, whereClause);
				
				while(objectCacheKeys.size() >= objectCacheMax) {
					/* Drop oldest element from cache */
					String oldCacheKey = (String)objectCacheKeys.elementAt(0);
					objectCacheKeys.removeElementAt(0);
					objectCache.remove(oldCacheKey);
				}
				
				/* Add element to cache */
				objectCacheKeys.addElement(cacheKey);
				objectCache.put(cacheKey, v);
			}
		return v;
		}
	}

	protected void setArguments(String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause, String whereClause)
	{
		this.setName(name);
		this.keyColumn = keyColumn;
		this.dataColumns = dataColumns;
		this.table = table;
		this.orderClause = orderClause;
		this.groupClause = groupClause;
		this.whereClause = whereClause;
	}
	
	protected static DatabaseView get(Class c, String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause)
	{
		return get(c, name, keyColumn, dataColumns, table, orderClause, groupClause, null);
	}

	protected static DatabaseView get(Class c, String name, String keyColumn, String[] dataColumns, String table, String orderClause)
	{
		return get(c, name, keyColumn, dataColumns, table, orderClause, null, null);
	}

	protected static DatabaseView get(Class c, String name, String keyColumn, String[] dataColumns, String table)
	{
		return get(c, name, keyColumn, dataColumns, table, null, null, null);
	}

	public static DatabaseView get(String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause, String whereClause)	{
		return get(DatabaseView.class, name, keyColumn, dataColumns, table, orderClause, groupClause, whereClause);
	}
	
	public static DatabaseView get(String name, String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause)	{
		return get(DatabaseView.class, name, keyColumn, dataColumns, table, orderClause, groupClause, null);
	}

	public static DatabaseView get(String name, String keyColumn, String[] dataColumns, String table, String orderClause)	{
		return get(DatabaseView.class, name, keyColumn, dataColumns, table, orderClause, null, null);
	}

	public static DatabaseView get(String name, String keyColumn, String[] dataColumns, String table)	{
		return get(DatabaseView.class, name, keyColumn, dataColumns, table, null, null, null);
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
		String result = constructQuery(this.keyColumn, this.dataColumns, this.table, this.orderClause, this.groupClause, this.whereClause);
		System.err.println(result);
		return result;
	}
	
	protected static String constructQuery(String keyColumn, String[] dataColumns, String table, String orderClause, String groupClause, String whereClause)
	{
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
	
	/* (non-Javadoc)
	 * @see ch.ploetzli.xbmc.j2me.SubMenu#select(int)
	 */
	protected void select(int index) {
		if(index >= 0 && index < cache.size()) {
			String[] row = (String[])cache.elementAt(index);
			select(row);
		}
	}
	
	/**
	 * Needs to be overriden in a subclass.
	 * @param row
	 */
	protected void select(String row[]) {
		
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
