package ch.ploetzli.xbmc.j2me;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A version of Hashtable that keeps track of which keys where last requested
 * through get() and removes the least recently requested object if a put() is
 * issued and a predetermined maximum number of elements is reached.
 * @author henryk
 *
 */
public class LRUHashtable extends Hashtable {
	
	/**
	 * Keep at most this many objects in the Hashtable.
	 * When residency reaches this number the oldest object, as determined
	 * by LRUList gets removed before a new one is inserted.
	 */
	private int maxElements;
	
	/**
	 * LRU list of keys, to ensure the limit of maxElements objects in the Hashtable.
	 * The vector contains the keys in ascending order of least-recent-usedness,
	 * e.g. the last element will be the most recently returned and the first
	 * object will be the least recently returned and next candidate for dropping.
	 */
	private Vector LRUList;

	public LRUHashtable(int maxElements) {
		super(maxElements);
		this.maxElements = maxElements;
		this.LRUList = new Vector(maxElements);
	}

	public synchronized void clear() {
		LRUList.removeAllElements();
		super.clear();
	}

	public synchronized Object put(Object key, Object value) {
		while(LRUList.size() >= maxElements) {
			/* Drop oldest element */
			Object oldKey = LRUList.elementAt(0);
			remove(oldKey);
		}
		return super.put(key, value);
	}

	public synchronized Object get(Object key) {
		if(containsKey(key)) {
			/* Append key to end of LRU list */
			LRUList.removeElement(key);
			LRUList.addElement(key);
		}
		return super.get(key);
	}
	
	public synchronized Object remove(Object key) {
		LRUList.removeElement(key);
		return super.remove(key);
	}
}
