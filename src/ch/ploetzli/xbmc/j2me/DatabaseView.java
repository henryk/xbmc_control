package ch.ploetzli.xbmc.j2me;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Ticker;

import ch.ploetzli.xbmc.LRUHashtable;
import ch.ploetzli.xbmc.Logger;
import ch.ploetzli.xbmc.Utils;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

/**
 * This class is a menu that represents some database derived view, e.g.
 * a list of submenus that represent database items. The default implementation
 * can execute a background query to the database and populate a List with
 * the results. No action on what should happen if one of the list items is
 * selected is defined though, so this class is generally subclassed with a
 * specific class that will know what to do when e.g. a year is selected.
 * 
 * No constructor is defined for this class and instances should be retrieved
 * with one of the get methods, which will access a singleton cache in order
 * to reuse objects which are constructed with the same arguments. This will
 * a) cache the database results in memory, and b) enable consistent user
 * experience by keeping the selection in each menu persistent over deselection
 * and reselection of a menu.
 * @author henryk
 *
 */
public class DatabaseView extends DatabaseSubMenu {
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
	protected static LRUHashtable objectCache = new LRUHashtable(35);
	
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
			v = (DatabaseView)objectCache.get(cacheKey);
			if(v == null) {
				/* Must construct new object */
				try {
					v = (DatabaseView)c.newInstance();
				} catch(Exception e) {
					/* Might as well crash and burn if anything goes wrong here. */
				}
				v.setArguments(name, keyColumn, dataColumns, table, orderClause, groupClause, whereClause);
				
				/* Add element to cache */
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
	 * Return our DatabaseView's cache validator.
	 * The cache is protected by a validator that is sum() and count() over the
	 * key column as well as a string representation of the database epoch. This
	 * should be guaranteed to catch any deletion and insertion of rows, since
	 * keys are strictly monotonic. Through the epoch it will also catch user
	 * initiated cache invalidation.
	 * @param topMenu A DatabaseTopMenu instance
	 * @return false Array of String to be matched against the cacheValidator
	 * 	instance variable.
	 */
	public String[] fetchCacheValidator(DatabaseTopMenu topMenu) throws IOException
	{
		RecordSetConnection conn = null;
		String newValidator[] = null;
		try {
			HttpApi api = topMenu.getApi();
			if(api != null) {
				/* TODO Maybe include where clause? */
				conn = api.queryVideoDatabase("select " + topMenu.getDatabaseEpoch() + ",count("+keyColumn+"), sum("+keyColumn+") from "+table);
				if(conn.hasMoreElements()) {
					Object o = conn.nextElement();
					if(o instanceof String[]) {
						newValidator = (String[])o;
					}
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
				DatabaseTopMenu topMenu = getDatabaseTopMenu();
				if(topMenu != null) {
					HttpApi api = topMenu.getApi();
					if(api != null) {
						String[] newValidator = fetchCacheValidator(topMenu);
						if(newValidator == null 
								|| cacheValidator == null 
								|| !Utils.stringArraysEqual(newValidator, cacheValidator)) {
							fetchData(api);

							if(!exit) 
								cacheValidator = newValidator;
						}
					}
				}
			} catch(Exception e) {
				Logger.getLogger().info(e);
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
		Logger.getLogger().info(result);
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
			Logger.getLogger().error("BUG: haveRecord("+i+"), but only "+cache.size()+" objects in cache");
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
			Logger.getLogger().error("BUG: haveRecordCount("+len+"), but only "+cache.size()+" objects in cache");
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
	 * Common code from appendRow and updateRow to be overriden in subclasses.
	 * The default implementation simply uses the first non-key column as a label.
	 * @param index
	 * @param data
	 * @return
	 */
	protected Object[] formatRow(int index, String[] data)
	{
		String label = "No label";
		if(data.length > 0) {
			label = data[0];
			if(keyColumn != null && data.length > 1) {
				label = data[1];
			}
		}
		return new Object[]{label, null};
	}
	
	/**
	 * Update GUI with new database row. The default implementation simply uses
	 * 	the first non-key column as a label, but subclasses might want to override
	 * @param index
	 * @param data
	 */
	protected void appendRow(int index, String[] data)
	{
		Object[] o = formatRow(index, data);
		((List)getDisplayable()).append((String)o[0], (Image)o[1]);
	}

	/**
	 * Update GUI with changed database row. The default implementation simply uses
	 * 	the first non-key column as a label, but subclasses might want to override
	 * @param index
	 * @param data
	 */
	protected void updateRow(int index, String[] data)
	{
		Object[] o = formatRow(index, data);
		((List)getDisplayable()).set(index, (String)o[0], (Image)o[1]);
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
